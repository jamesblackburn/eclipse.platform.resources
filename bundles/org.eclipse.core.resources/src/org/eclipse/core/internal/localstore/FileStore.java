package org.eclipse.core.internal.localstore;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public abstract class FileStore {

/**
 * Creates a folder in the store.
 */
abstract public FileObject getFile(IResource target) throws CoreException;

/**
 * Creates a folder in the store.
 */
abstract public void write(IFolder target) throws CoreException;

}