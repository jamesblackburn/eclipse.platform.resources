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

import org.eclipse.core.filesystem.FileStore;
import org.eclipse.core.runtime.IPath;

/**
 * Represents the root of a file system that is connected to the workspace.
 * A file system can be rooted on any resource.
 */
public class FileStoreRoot {
	private int chop;
	/**
	 * When a root is changed, the old root object is marked invalid
	 * so that other resources with a cache of the root will know they need to update.
	 */
	private boolean isValid = true;
	private FileStore root;

	/**
	 * Defines the root of a file system within the workspace tree.
	 * @param root The virtual file representing the root of the file
	 * system that has been mounted
	 * @param workspacePath The workspace path at which this file
	 * system has been mounted
	 */
	FileStoreRoot(FileStore root, IPath workspacePath) {
		this.root = root;
		this.chop = workspacePath.segmentCount();
	}

	FileStore getFileSystemObject(IPath workspacePath) {
		if (workspacePath.segmentCount() <= chop)
			return root;
		return root.getChild(workspacePath.removeFirstSegments(chop).toOSString());
	}

	boolean isValid() {
		return isValid;
	}

	void setValid(boolean isValid) {
		this.isValid = isValid;
	}
}