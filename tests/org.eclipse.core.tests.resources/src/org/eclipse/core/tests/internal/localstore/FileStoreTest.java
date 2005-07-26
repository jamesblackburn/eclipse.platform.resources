/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import java.io.File;
import java.util.Date;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;

public class FileStoreTest extends LocalStoreTest {
	public static Test suite() {
		return new TestSuite(FileStoreTest.class);
	}

	public FileStoreTest() {
		super();
	}

	public FileStoreTest(String name) {
		super(name);
	}

	private FileStore createDir(FileStore store, boolean clear) throws CoreException {
		if (clear && store.fetchInfo().exists())
			store.delete(NONE, null);
		store.create(NONE, null);
		IFileInfo info = store.fetchInfo();
		assertTrue("createDir.1", info.exists());
		assertTrue("createDir.1", info.isDirectory());
		return store;
	}

	private FileStore createDir(String string, boolean clear) throws CoreException {
		return createDir(FileStoreFactory.create(new Path(string)), clear);
	}

	/**
	 * Basically this is a test for the Windows Platform.
	 */
	public void testCopyAcrossVolumes() throws Throwable {

		/* test if we are in the adequate environment */
		if (!new File("c:\\").exists() || !new File("d:\\").exists())
			return;

		/* build scenario */
		// create source root folder
		FileStore tempC = createDir("c:/temp", false);
		// create destination root folder
		FileStore tempD = createDir("d:/temp", false);
		// create tree
		FileStore target = tempC.getChild("target");
		createDir(target, true);
		createTree(getTree(target));

		/* c:\temp\target -> d:\temp\target */
		FileStore destination = tempD.getChild("target");
		target.copy(destination, NONE, null);
		assertTrue("3.1", verifyTree(getTree(destination)));
		destination.delete(NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		destination = tempD.getChild("copy of target");
		target.copy(destination, NONE, null);
		assertTrue("4.1", verifyTree(getTree(destination)));
		destination.delete(NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file) */
		destination = tempD.getChild("target");
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		try {
			target.copy(destination, NONE, null);
			fail("5.2");
		} catch (CoreException e) {
			//should fail
		}
		assertTrue("5.3", !verifyTree(getTree(destination)));
		destination.delete(IFileStoreConstants.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempD.getChild("target");
		createDir(destination, true);
		target.copy(destination, NONE, null);
		assertTrue("6.2", verifyTree(getTree(destination)));
		destination.delete(IFileStoreConstants.NONE, null);

		/* remove trash */
		target.delete(IFileStoreConstants.NONE, null);
	}

	public void testCopyDirectory() throws Throwable {
		/* build scenario */
		FileStore temp = FileStoreFactory.create(getWorkspace().getRoot().getLocation().append("temp"));
		temp.create(DIRECTORY, null);
		assertTrue("1.1", temp.fetchInfo().isDirectory());
		// create tree
		FileStore target = temp.getChild("target");
		target.delete(NONE, null);
		createTree(getTree(target));

		/* temp\target -> temp\copy of target */
		FileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, NONE, null);
		assertTrue("2.1", verifyTree(getTree(copyOfTarget)));

		/* remove trash */
		target.delete(NONE, null);
		copyOfTarget.delete(NONE, null);
	}

	public void testCopyFile() throws Throwable {
		/* build scenario */
		FileStore temp = createDir(getWorkspace().getRoot().getLocation().append("temp").toString(), true);
		// create target
		String content = "this is just a simple content \n to a simple file \n to test a 'simple' copy";
		FileStore target = temp.getChild("target");
		target.delete(IFileStoreConstants.NONE, null);
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		assertTrue("1.4", compareContent(getContents(content), target.openInputStream(NONE)));

		/* temp\target -> temp\copy of target */
		FileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
		assertTrue("2.1", compareContent(getContents(content), copyOfTarget.openInputStream(NONE)));
		copyOfTarget.delete(NONE, null);

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this part of the test.
		if (usingNatives()) {
			/* make source read-only and try the copy temp\target -> temp\copy of target */
			copyOfTarget = temp.getChild("copy of target");
			setReadOnly(target, true);

			target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
			assertTrue("3.1", compareContent(getContents(content), copyOfTarget.openInputStream(NONE)));
			// reset read only flag for cleanup
			setReadOnly(copyOfTarget, false);
			copyOfTarget.delete(NONE, null);
			// reset the read only flag for cleanup
			setReadOnly(target, false);
			target.delete(IFileStoreConstants.NONE, null);
		}

		/* copy a big file to test progress monitor */
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 1000; i++)
			sb.append("asdjhasldhaslkfjhasldkfjhasdlkfjhasdlfkjhasdflkjhsdaf");
		FileStore bigFile = temp.getChild("bigFile");
		createFile(bigFile, sb.toString());
		assertTrue("7.1", bigFile.fetchInfo().exists());
		assertTrue("7.2", compareContent(getContents(sb.toString()), bigFile.openInputStream(NONE)));
		FileStore destination = temp.getChild("copy of bigFile");
		//IProgressMonitor monitor = new LoggingProgressMonitor(System.out);
		IProgressMonitor monitor = getMonitor();
		bigFile.copy(destination, NONE, monitor);
		assertTrue("7.3", compareContent(getContents(sb.toString()), destination.openInputStream(NONE)));
		destination.delete(NONE, null);

		/* take out the trash */
		temp.delete(IFileStoreConstants.NONE, null);
	}

	/**
	 * Basically this is a test for the Windows Platform.
	 */
	public void testCopyFileAcrossVolumes() throws Throwable {
		/* test if we are in the adequate environment */
		if (!new File("c:\\").exists() || !new File("d:\\").exists())
			return;

		/* build scenario */
		// create source
		FileStore tempC = createDir("c:\\temp", false);
		// create destination
		FileStore tempD = createDir("d:\\temp", false);
		// create target
		String content = "this is just a simple content \n to a simple file \n to test a 'simple' copy";
		FileStore target = tempC.getChild("target");
		target.delete(IFileStoreConstants.NONE, null);
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		assertTrue("1.4", compareContent(getContents(content), target.openInputStream(NONE)));

		/* c:\temp\target -> d:\temp\target */
		FileStore destination = tempD.getChild("target");
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("3.1", compareContent(getContents(content), destination.openInputStream(NONE)));
		destination.delete(NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		destination = tempD.getChild("copy of target");
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("4.1", compareContent(getContents(content), destination.openInputStream(NONE)));
		destination.delete(NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file */
		destination = tempD.getChild("target");
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("5.2", compareContent(getContents(content), destination.openInputStream(NONE)));
		destination.delete(NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempD.getChild("target");
		createDir(destination, true);
		assertTrue("6.1", destination.fetchInfo().isDirectory());
		boolean ok = false;
		try {
			target.copy(destination, NONE, null);
		} catch (CoreException e) {
			/* test if the input stream inside the copy method was closed */
			target.delete(NONE, null);
			createFile(target, content);
			ok = true;
		}
		assertTrue("6.2", ok);
		assertTrue("6.3", destination.fetchInfo().isDirectory());
		destination.delete(NONE, null);

		/* remove trash */
		target.delete(NONE, null);
	}

	public void testGetStat() throws CoreException {
		/* evaluate test environment */
		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		FileStore temp = createDir(root.toString(), true);

		/* create common objects */
		FileStore target = temp.getChild("target");
		long stat;

		/* test stat with an inexisting file */
		stat = target.fetchInfo().getLastModified();
		assertTrue("1.0", stat == 0);

		/* test stat with an existing folder */
		createDir(target, true);
		stat = target.fetchInfo().getLastModified();
		assertTrue("2.0", stat != 0);

		/* remove trash */
		temp.delete(NONE, null);
	}

	public void testMove() throws Throwable {
		/* build scenario */
		FileStore tempC = createDir(getWorkspace().getRoot().getLocation().append("temp").toString(), true);
		// create target file
		FileStore target = tempC.getChild("target");
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		// create target tree
		FileStore tree = tempC.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* rename file */
		FileStore destination = tempC.getChild("destination");
		target.move(destination, NONE, null);
		assertTrue("2.1", !destination.fetchInfo().isDirectory());
		assertTrue("2.2", !target.fetchInfo().exists());
		destination.move(target, NONE, null);
		assertTrue("2.3", !target.fetchInfo().isDirectory());
		assertTrue("2.4", !destination.fetchInfo().exists());

		/* rename file (but destination is already a file) */
		String anotherContent = "another content";
		createFile(destination, anotherContent);
		boolean ok = false;
		try {
			target.move(destination, NONE, null);
		} catch (CoreException e) {
			ok = true;
		}
		assertTrue("3.1", ok);
		assertTrue("3.2", !target.fetchInfo().isDirectory());
		destination.delete(NONE, null);
		assertTrue("3.3", !destination.fetchInfo().exists());

		/* rename file (but destination is already a folder) */
		createDir(destination, true);
		try {
			target.move(destination, NONE, null);
		} catch (CoreException e) {
			ok = true;
		}
		assertTrue("4.1", ok);
		assertTrue("4.2", !target.fetchInfo().isDirectory());
		destination.delete(NONE, null);
		assertTrue("4.3", !destination.fetchInfo().exists());

		/* rename folder */
		destination = tempC.getChild("destination");
		tree.move(destination, NONE, null);
		assertTrue("6.1", verifyTree(getTree(destination)));
		assertTrue("6.2", !tree.fetchInfo().exists());
		destination.move(tree, NONE, null);
		assertTrue("6.3", verifyTree(getTree(tree)));
		assertTrue("6.4", !destination.fetchInfo().exists());

		/* remove trash */
		target.delete(IFileStoreConstants.NONE, null);
		tree.delete(IFileStoreConstants.NONE, null);
	}

	public void testMoveAcrossVolumes() throws Throwable {
		/* test if we are in the adequate environment */
		if (!new File("c:\\").exists() || !new File("d:\\").exists())
			return;

		/* build scenario */
		// create source
		FileStore tempC = createDir("c:\\temp", false);
		// create destination
		FileStore tempD = createDir("d:\\temp", false);
		// create target file
		FileStore target = tempC.getChild("target");
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		// create target tree
		FileStore tree = tempC.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* move file across volumes */
		FileStore destination = tempD.getChild("target");
		target.move(destination, NONE, null);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		assertTrue("5.2", !target.fetchInfo().exists());
		destination.move(target, NONE, null);
		assertTrue("5.3", !target.fetchInfo().isDirectory());
		assertTrue("5.4", !destination.fetchInfo().exists());

		/* move folder across volumes */
		destination = tempD.getChild("target");
		tree.move(destination, NONE, null);
		assertTrue("9.1", verifyTree(getTree(destination)));
		assertTrue("9.2", !tree.fetchInfo().exists());
		destination.move(tree, NONE, null);
		assertTrue("9.3", verifyTree(getTree(tree)));
		assertTrue("9.4", !destination.fetchInfo().exists());

		/* remove trash */
		target.delete(IFileStoreConstants.NONE, null);
		tree.delete(IFileStoreConstants.NONE, null);
	}

	public void testReadOnly() throws CoreException {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		if (!usingNatives())
			return;

		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		FileStore targetFolder = createDir(root.toString(), true);
		FileStore targetFile = targetFolder.getChild("targetFile");
		try {
			createFileInFileSystem(targetFile);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// file
		assertTrue("1.0", !targetFile.fetchInfo().isReadOnly());
		setReadOnly(targetFile, true);
		assertTrue("1.2", targetFile.fetchInfo().isReadOnly());
		setReadOnly(targetFile, false);
		assertTrue("1.4", !targetFile.fetchInfo().isReadOnly());

		// folder
		assertTrue("2.0", !targetFolder.fetchInfo().isReadOnly());
		setReadOnly(targetFolder, true);
		assertTrue("2.2", targetFolder.fetchInfo().isReadOnly());
		setReadOnly(targetFolder, false);
		assertTrue("2.4", !targetFolder.fetchInfo().isReadOnly());

		/* remove trash */
		targetFolder.delete(NONE, null);
	}
}
