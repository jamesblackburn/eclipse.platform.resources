package org.eclipse.core.internal.localstore;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Abstract representation of a file. This is the object managed by
 * the FileManager.
 */

public abstract class FileObject {

abstract public boolean exists();

abstract public boolean isFile();

abstract public boolean isFolder();

abstract public long getLastModified();

abstract public String getLocation();

/**
 * Clean all the cached information about this file.
 */
abstract public void refresh();
}