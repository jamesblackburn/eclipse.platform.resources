package org.eclipse.core.internal.localstore;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.internal.filesystem.FileSystemStore2;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class should only contain algorithms to execute file
 * management. The "real work" will be done by a separate
 * store like the FileSystemStore or DavStore.
 */

// FIXME: should remove the superclass later
public class FileManager extends FileSystemResourceManager {

protected FileStore fileStore;

public FileManager(Workspace workspace) {
	super(workspace);
	fileStore = new FileSystemStore2();
}

public FileManager(Workspace workspace, FileStore fileStore) {
	super(workspace);
	this.fileStore = fileStore;
}

public void write(IFolder target, boolean force, IProgressMonitor monitor) throws CoreException {
	FileObject file = fileStore.getFile(target);
	if (!force)
		if (file.isFolder()) {
			String message = Policy.bind("localstore.resourceExists", target.getFullPath().toString());
			throw new ResourceException(IResourceStatus.EXISTS_LOCAL, target.getFullPath(), message, null);
		} else {
			if (file.exists()) {
				String message = Policy.bind("localstore.fileExists", target.getFullPath().toString());
				throw new ResourceException(IResourceStatus.OUT_OF_SYNC_LOCAL, target.getFullPath(), message, null);
			}
		}
	fileStore.write(target);
	file.refresh();
	ResourceInfo info = ((Resource) target).getResourceInfo(false, true);
	updateLocalSync(info, file.getLastModified(), false);
}
}