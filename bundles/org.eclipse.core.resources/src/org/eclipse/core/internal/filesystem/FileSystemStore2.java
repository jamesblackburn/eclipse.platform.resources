package org.eclipse.core.internal.filesystem;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

// FIXME: no internal import should be here
import org.eclipse.core.internal.localstore.FileObject;
import org.eclipse.core.internal.localstore.FileStore;
import org.eclipse.core.internal.localstore.FileSystemStore;
import org.eclipse.core.internal.utils.Policy;

// OK imports
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import java.io.File;

// FIXME: should only extend FileStore
// FIXME: should be renamed to FileSystemStore
public class FileSystemStore2 extends FileSystemStore {

public FileObject getFile(IResource target) throws CoreException {
	// FIXME: use locationFor instead of getLocation()
	return new FileSystemFile(target.getLocation().toOSString());
}

public void write(IFolder target) throws CoreException {
	// FIXME: use locationFor instead of getLocation()
	File folder = target.getLocation().toFile();
	if (!folder.exists())
		folder.mkdirs();
	if (!folder.isDirectory()) {
		String message;
		if (folder.isFile())
			message = "Resource is not a folder: " + folder.getAbsolutePath();
		else
			message = Policy.bind("localstore.couldNotCreateFolder", folder.getAbsolutePath());
		IStatus status = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_WRITE_LOCAL, message, null);
		throw new CoreException(status);
	}
}
}