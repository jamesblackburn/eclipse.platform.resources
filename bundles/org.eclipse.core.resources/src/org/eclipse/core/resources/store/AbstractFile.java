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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A resource store stores and retrieves files from some persistent storage,
 * such as a disk or network connection.
 */
public abstract class AbstractFile {

	public abstract AbstractFile[] children(int options) throws CoreException;

	public abstract void copy(AbstractFile destination, int options, IProgressMonitor monitor) throws CoreException;

	public abstract void create(int options) throws CoreException;

	public abstract void delete(int options) throws CoreException;

	public abstract boolean equals(Object obj);
	
	public abstract boolean exists();

	public abstract int hashCode();

	public abstract boolean isDirectory();

	public abstract boolean isReadOnly();

	public abstract long lastModified();

	public abstract void move(AbstractFile destination, int options, IProgressMonitor monitor) throws CoreException;

	public abstract InputStream openInputStream(int options) throws CoreException;

	public abstract OutputStream openOutputStream(int options) throws CoreException;

	public abstract void setLastModified(long value) throws CoreException;
}
