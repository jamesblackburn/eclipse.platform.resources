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
import org.eclipse.core.resources.store.AbstractFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * 
 */
public class LocalFile extends AbstractFile {
	private final File file;

	public LocalFile(File file) {
		super();
		this.file = file;
	}

	public AbstractFile[] children(int options) throws CoreException {
		return null;
	}

	public void copy(AbstractFile destination, int options, IProgressMonitor monitor) throws CoreException {
	}

	public void create(int options) throws CoreException {
	}

	public void delete(int options) throws CoreException {
	}

	public boolean equals(Object obj) {
		return false;
	}

	public boolean exists() {
		return false;
	}

	public int hashCode() {
		return 0;
	}

	public boolean isDirectory() {
		return false;
	}

	public boolean isReadOnly() {
		return false;
	}

	public long lastModified() {
		return 0;
	}

	public void move(AbstractFile destination, int options, IProgressMonitor monitor) throws CoreException {
	}

	public InputStream openInputStream(int options) throws CoreException {
		return null;
	}

	public OutputStream openOutputStream(int options) throws CoreException {
		return null;
	}

	public void setLastModified(long value) throws CoreException {
	}

}
