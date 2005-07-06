/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.resources.store;

import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IPath;

/**
 * 
 */
public abstract class AbstractFileSystem {
	
	public static final int ATTRIBUTE_READ_ONLY = 1;
	
	public static final int NONE = 0;
	public static final int APPEND = 1;
	public static final int OVERWRITE = 2;
	
	public static final int FILE = 1;
	public static final int FOLDER = 2;

	
	private final IPath parentPath;

	public AbstractFileSystem(IPath parentPath) {
		super();
		this.parentPath = parentPath;
	}

	public abstract int attributes(Object fsObject);

	public abstract String[] childNames(Object fsObject, int options);

	public abstract Object[] children(Object fsObject, int options) throws CoreException;

	public abstract void copy(Object source, Object destination, int options, IProgressMonitor monitor) throws CoreException;

	public abstract void create(Object fsObject, int options) throws CoreException;

	public abstract void delete(Object fsObject, int options) throws CoreException;

	public abstract boolean exists(Object fsObject);

	public abstract Object getFileSystemObject(IPath pathInParent);

	protected IPath getParentPath() {
		return parentPath;
	}

	public abstract boolean isDirectory(Object fsObject);

	public abstract long lastModified(Object fsObject);

	public abstract void move(Object source, Object destination, int options, IProgressMonitor monitor) throws CoreException;

	public abstract String name(Object fsObject);

	public abstract InputStream openInputStream(Object fsObject, int options) throws CoreException;

	public abstract OutputStream openOutputStream(Object fsObject, int options) throws CoreException;

	public abstract void setAttributes(Object fsObject, int value);

	public abstract void setLastModified(Object fsObject, long value) throws CoreException;
}
