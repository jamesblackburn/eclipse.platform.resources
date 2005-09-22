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

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.FileSystemCore;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Represents the root of a file system that is connected to the workspace.
 * A file system can be rooted on any resource.
 */
public class FileStoreRoot {
	private final int chop;
	/**
	 * When a root is changed, the old root object is marked invalid
	 * so that other resources with a cache of the root will know they need to update.
	 */
	private boolean isValid = true;
	private final URI root;
	private final String rawRootPath;
	private final IPathVariableManager variableManager;

	/**
	 * Defines the root of a file system within the workspace tree.
	 * @param root The virtual file representing the root of the file
	 * system that has been mounted
	 * @param workspacePath The workspace path at which this file
	 * system has been mounted
	 */
	FileStoreRoot(URI root, IPath workspacePath) {
		this.root = root;
		String raw = root.getPath();
		//make sure root has trailing slash
		if (raw.charAt(raw.length() - 1) != '/')
			raw += '/';
		this.rawRootPath = raw;
		this.chop = workspacePath.segmentCount();
		this.variableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
	}

	private URI buildURI(IPath workspacePath) {
		int count = workspacePath.segmentCount();
		if (count <= chop)
			return root;
		StringBuffer result = new StringBuffer(rawRootPath);
		for (int i = chop; i < count; i++) {
			result.append(workspacePath.segment(i));
			result.append('/');
		}
		try {
			return new URI(root.getScheme(), root.getHost(), result.toString(), null);
		} catch (URISyntaxException e) {
			//this workspace path could not be represented as a URI - is this possible?
			throw new Error(e);
		}
	}

	IFileStore createStore(IPath workspacePath) {
		URI uri = variableManager.resolveURI(buildURI(workspacePath));
		try {
			//scheme is null for undefined path variables
			if (uri.getScheme() != null)
				return FileSystemCore.getStore(uri);
		} catch (CoreException e) {
			//the resource location has an undefined or invalid protocol - fall through below
		}
		return FileSystemCore.getNullFileSystem().getStore(workspacePath);
	}

	boolean isValid() {
		return isValid;
	}

	void setValid(boolean value) {
		this.isValid = value;
	}
}