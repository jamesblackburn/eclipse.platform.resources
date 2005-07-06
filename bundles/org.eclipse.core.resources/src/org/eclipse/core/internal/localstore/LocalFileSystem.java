/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.*;
import java.io.File;
import java.net.URL;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.store.AbstractFileSystem;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * File system implementation based on storage of files in the local
 * operating system's file system.
 */
public class LocalFileSystem extends AbstractFileSystem {
	private static final Object[] EMPTY_ARRAY = new Object[0];
	/**
	 * Singleton buffer created to prevent buffer creations in the
	 * transferStreams method.  Used as an optimization, based on the assumption
	 * that multiple writes won't happen in a given instance of FileSystemStore.
	 */
	private final byte[] buffer = new byte[8192];

	private int chop;

	private File root;

	public LocalFileSystem(URL rootURL, IPath parentPath) {
		super(parentPath);
		this.root = new File(rootURL.getFile());
		this.chop = parentPath.segmentCount();
	}

	public int attributes(Object fsObject) {
		return 0;
		//TODO
	}

	/**
	 * This method is called after a failure to modify a file or directory.
	 * Check to see if the parent is read-only and if so then
	 * throw an exception with a more specific message and error code.
	 * 
	 * @param target The file that we failed to modify
	 * @param exception The low level exception that occurred, or <code>null</code>
	 * @throws CoreException A more specific exception if the parent is read-only
	 */
	private void checkReadOnlyParent(File target, Throwable exception) throws CoreException {
		String parent = target.getParent();
		if (parent != null && (attributes(parent) & ATTRIBUTE_READ_ONLY) != 0) {
			String message = NLS.bind(Messages.localstore_readOnlyParent, target.getAbsolutePath());
			throw new ResourceException(IResourceStatus.PARENT_READ_ONLY, null, message, exception);
		}
	}

	public String[] childNames(Object fsObject, int options) {
		String[] names = ((File) fsObject).list();
		return (String[]) (names == null ? EMPTY_ARRAY : names);
	}

	public Object[] children(Object fsObject, int options) {
		Object[] children = ((File) fsObject).listFiles();
		return children == null ? EMPTY_ARRAY : children;
	}

	public void copy(Object srcObject, Object destObject, int options, IProgressMonitor monitor) throws CoreException {
		File source = (File) srcObject;
		File destination = (File) destObject;
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(NLS.bind(Messages.localstore_copying, source.getAbsolutePath()), 1);
			Policy.checkCanceled(monitor);
			if (source.isDirectory())
				copyDirectory(source, destination, Policy.subMonitorFor(monitor, 1));
			else
				copyFile(source, destination, Policy.subMonitorFor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	private void copyDirectory(File source, File destination, IProgressMonitor monitor) throws CoreException {
		try {
			String[] children = childNames(source, NONE);

			monitor.beginTask(NLS.bind(Messages.localstore_copying, source.getAbsolutePath()), children.length);
			// create directory
			create(destination, NONE);

			// copy children
			for (int i = 0; i < children.length; i++)
				copy(new File(source, children[i]), new File(destination, children[i]), NONE, Policy.subMonitorFor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	private void copyFile(File source, File destination, IProgressMonitor monitor) throws CoreException {
		try {
			int totalWork = 1 + ((int) source.length() / 8192);
			String sourcePath = source.getAbsolutePath();
			monitor.beginTask(NLS.bind(Messages.localstore_copying, sourcePath), totalWork);
			try {
				create(source.getParentFile(), FOLDER);
				InputStream in = openInputStream(source, NONE);
				OutputStream out = openOutputStream(destination, NONE);
				transferStreams(in, out, sourcePath, monitor);
			} catch (CoreException e) {
				//if we failed to write, try to cleanup the half written file
				if (!destination.isDirectory())
					destination.delete();
				throw e;
			}
			// update the destination timestamp on disk
			long stat = CoreFileSystemLibrary.getStat(sourcePath);
			long lastModified = CoreFileSystemLibrary.getLastModified(stat);
			destination.setLastModified(lastModified);
			// update file attributes
			CoreFileSystemLibrary.copyAttributes(sourcePath, destination.getAbsolutePath(), false);
		} finally {
			monitor.done();
		}
	}

	public void create(Object fsObject, int options) throws CoreException {
		File target = (File) fsObject;
		if ((options & FILE) != 0) {
			try {
				target.createNewFile();
			} catch (IOException e) {
				checkReadOnlyParent(target, e);
				String message;
				if (target.isDirectory())
					message = NLS.bind(Messages.localstore_notAFile, target.getAbsolutePath());
				else
					message = NLS.bind(Messages.localstore_couldNotWrite, target.getAbsolutePath());
				throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, null, message, e);
			}
		}
		//must be a folder
		target.mkdirs();
		if (!target.isDirectory()) {
			checkReadOnlyParent(target, null);
			String message = NLS.bind(Messages.localstore_couldNotCreateFolder, target.getAbsolutePath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, null, message, null);
		}
	}

	public void delete(Object fsObject, int options) throws CoreException {
		String message = Messages.localstore_deleteProblemDuringMove;
		MultiStatus result = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_DELETE_LOCAL, message, null);
		internalDelete(root, root.getAbsolutePath(), result);
		if (!result.isOK())
			throw new CoreException(result);
	}

	public boolean exists(Object fsObject) {
		return ((File) fsObject).exists();
	}

	public Object getFileSystemObject(IPath pathInParent) {
		return new File(root, pathInParent.removeFirstSegments(chop).toOSString());
	}

	/**
	 * Deletes the given file recursively, adding failure info to
	 * the provided status object.  The filePath is passed as a parameter
	 * to optimize java.io.File object creation.
	 */
	private boolean internalDelete(File target, String filePath, MultiStatus status) {
		boolean failedRecursive = false;
		if (target.isDirectory()) {
			String[] list = childNames(target, NONE);
			int parentLength = filePath.length();
			for (int i = 0, imax = list.length; i < imax; i++) {
				//optimized creation of child path object
				StringBuffer childBuffer = new StringBuffer(parentLength + list[i].length() + 1);
				childBuffer.append(filePath);
				childBuffer.append(File.separatorChar);
				childBuffer.append(list[i]);
				String childName = childBuffer.toString();
				// try best effort on all children so put logical OR at end
				failedRecursive = !internalDelete(new java.io.File(childName), childName, status) || failedRecursive;
			}
		}
		boolean failedThis = false;
		try {
			// don't try to delete the root if one of the children failed
			if (!failedRecursive && target.exists())
				failedThis = !target.delete();
		} catch (Exception e) {
			// we caught a runtime exception so log it
			String message = NLS.bind(Messages.localstore_couldnotDelete, target.getAbsolutePath());
			status.add(new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, new Path(target.getAbsolutePath()), message, e));
			return false;
		}
		if (failedThis) {
			String message = null;
			if (CoreFileSystemLibrary.isReadOnly(target.getAbsolutePath()))
				message = NLS.bind(Messages.localstore_couldnotDeleteReadOnly, target.getAbsolutePath());
			else
				message = NLS.bind(Messages.localstore_couldnotDelete, target.getAbsolutePath());
			status.add(new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, new Path(target.getAbsolutePath()), message, null));
		}
		return !(failedRecursive || failedThis);
	}

	public boolean isDirectory(Object fsObject) {
		return ((File) fsObject).isDirectory();
	}

	public long lastModified(Object fsObject) {
		return CoreFileSystemLibrary.getLastModified(((File) fsObject).getAbsolutePath());
	}

	public void move(Object srcObject, Object destObject, int options, IProgressMonitor monitor) throws CoreException {
		File source = (File) srcObject;
		File destination = (File) destObject;
		boolean overwrite = (options & OVERWRITE) != 0;
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(NLS.bind(Messages.localstore_moving, source.getAbsolutePath()), 2);
			//this flag captures case renaming on a case-insensitive OS, or moving
			//two equivalent files in an environment that supports symbolic links.
			//in these cases we NEVER want to delete anything
			boolean sourceEqualsDest = false;
			try {
				sourceEqualsDest = source.getCanonicalFile().equals(destination.getCanonicalFile());
			} catch (IOException e) {
				String message = NLS.bind(Messages.localstore_couldNotMove, source.getAbsolutePath());
				throw new ResourceException(new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, new Path(source.getAbsolutePath()), message, e));
			}
			if (!sourceEqualsDest && !overwrite && destination.exists()) {
				String message = NLS.bind(Messages.localstore_resourceExists, destination.getAbsolutePath());
				throw new ResourceException(IResourceStatus.EXISTS_LOCAL, new Path(destination.getAbsolutePath()), message, null);
			}
			if (source.renameTo(destination)) {
				// double-check to ensure we really did move
				// since java.io.File#renameTo sometimes lies
				if (!sourceEqualsDest && source.exists()) {
					// XXX: document when this occurs
					if (destination.exists()) {
						// couldn't delete the source so remove the destination
						// and throw an error
						// XXX: if we fail deleting the destination, the destination (root) may still exist
						Workspace.clear(destination);
						String message = NLS.bind(Messages.localstore_couldnotDelete, source.getAbsolutePath());
						throw new ResourceException(new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, new Path(source.getAbsolutePath()), message, null));
					}
					// source exists but destination doesn't so try to copy below
				} else {
					if (!destination.exists()) {
						// neither the source nor the destination exist. this is REALLY bad
						String message = NLS.bind(Messages.localstore_failedMove, source.getAbsolutePath(), destination.getAbsolutePath());
						throw new ResourceException(new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, new Path(source.getAbsolutePath()), message, null));
					}
				}
			}
			// for some reason we couldn't move - workaround: copy and delete the source
			// but if just case-renaming on a case-insensitive FS, there is no workaround 
			if (sourceEqualsDest) {
				String message = NLS.bind(Messages.localstore_couldNotMove, source.getAbsolutePath());
				throw new ResourceException(new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, new Path(source.getAbsolutePath()), message, null));
			}
			boolean success = false;
			boolean canceled = false;
			try {
				copy(source, destination, IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 1));
				success = true;
			} catch (OperationCanceledException e) {
				canceled = true;
				throw e;
			} finally {
				if (success) {
					// fail if source cannot be successfully deleted
					delete(source, NONE);
				} else {
					if (!canceled) {
						// We do not want to delete the destination in case of failure. It might
						// the case where we already had contents in the destination, so we would
						// be deleting resources we don't know about and the user might lose data.
						String message = NLS.bind(Messages.localstore_couldNotMove, source.getAbsolutePath());
						throw new ResourceException(new ResourceStatus(IResourceStatus.FAILED_WRITE_LOCAL, new Path(source.getAbsolutePath()), message, null));
					}
				}
			}
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	public String name(Object fsObject) {
		return ((File) fsObject).getName();
	}

	public InputStream openInputStream(Object fsObject, int options) throws CoreException {
		File target = (File) fsObject;
		try {
			return new FileInputStream(target);
		} catch (FileNotFoundException e) {
			String message;
			if (!target.exists())
				message = NLS.bind(Messages.localstore_fileNotFound, target.getAbsolutePath());
			else if (target.isDirectory())
				message = NLS.bind(Messages.localstore_notAFile, target.getAbsolutePath());
			else
				message = NLS.bind(Messages.localstore_couldNotRead, target.getAbsolutePath());
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, new Path(target.getAbsolutePath()), message, e);
		}
	}

	public OutputStream openOutputStream(Object fsObject, int options) throws CoreException {
		File target = (File) fsObject;
		String path = target.getAbsolutePath();
		try {
			return new FileOutputStream(path, (options & APPEND) != 0);
		} catch (FileNotFoundException e) {
			String message;
			int code = IResourceStatus.FAILED_WRITE_LOCAL;
			// Check to see if the parent is a read-only folder and if so then
			// throw an exception with a more specific message and error code.
			String parent = target.getParent();
			if (parent != null && CoreFileSystemLibrary.isReadOnly(parent)) {
				message = NLS.bind(Messages.localstore_readOnlyParent, path);
				code = IResourceStatus.PARENT_READ_ONLY;
			} else if (target.isDirectory())
				message = NLS.bind(Messages.localstore_notAFile, path);
			else
				message = NLS.bind(Messages.localstore_couldNotWrite, path);
			throw new ResourceException(code, new Path(path), message, e);
		}
	}

	public void setAttributes(Object fsObject, int value) {
	}

	public void setLastModified(Object fsObject, long value) throws CoreException {
		((File) fsObject).setLastModified(value);
	}

	/**
	 * Transfers all available bytes from the given input stream to the given output stream. 
	 * Regardless of failure, this method closes both streams.
	 * @param path The path of the object being copied, may be null
	 */
	public void transferStreams(InputStream source, OutputStream destination, String path, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			/*
			 * Note: although synchronizing on the buffer is thread-safe,
			 * it may result in slower performance in the future if we want 
			 * to allow concurrent writes.
			 */
			synchronized (buffer) {
				while (true) {
					int bytesRead = -1;
					try {
						bytesRead = source.read(buffer);
					} catch (IOException e) {
						String msg = NLS.bind(Messages.localstore_failedReadDuringWrite, path);
						IPath p = path == null ? null : new Path(path);
						throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, p, msg, e);
					}
					if (bytesRead == -1)
						break;
					try {
						destination.write(buffer, 0, bytesRead);
					} catch (IOException e) {
						String msg = NLS.bind(Messages.localstore_couldNotWrite, path);
						IPath p = path == null ? null : new Path(path);
						throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, p, msg, e);
					}
					monitor.worked(1);
				}
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			} finally {
				//close destination in finally in case source.close fails
				try {
					destination.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
}