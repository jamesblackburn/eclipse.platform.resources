package org.eclipse.core.internal.filesystem;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.internal.localstore.CoreFileSystemLibrary;
import org.eclipse.core.internal.localstore.FileObject;

public class FileSystemFile extends FileObject {

protected String location;
protected long stat;

public FileSystemFile(String location) {
	this.location = location;
	stat = CoreFileSystemLibrary.getStat(location);
}

public boolean exists() {
	return CoreFileSystemLibrary.isFile(stat) || CoreFileSystemLibrary.isFolder(stat);
}

public boolean isFile() {
	return CoreFileSystemLibrary.isFile(stat);
}

public boolean isFolder() {
	return CoreFileSystemLibrary.isFolder(stat);
}

public long getLastModified() {
	return CoreFileSystemLibrary.getLastModified(stat);
}

public String getLocation() {
	return location;
}

public void refresh() {
	stat = CoreFileSystemLibrary.getStat(location);
}
}