/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.indexing;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

public class PageStore implements Observer {
	private static final int CurrentPageStoreVersion = 1; // version 1

	private static final int NumberOfMetadataAreas = 16; // NEVER change this
	private static final int SizeOfMetadataArea = 64; // NEVER change this
	private static final byte[] ZEROES = new byte[1024];
	private Map acquiredPages;
	private RandomAccessFile file;
	private byte[] metadataBuffer;
	private Map modifiedPages;

	private String name;
	private int numberOfCacheHits;
	private int numberOfFileReads;
	private int numberOfFileWrites;
	private int numberOfPages;
	private int numberOfReads;
	private int numberOfWrites;
	private byte[] pageBuffer;
	private AbstractPagePolicy policy;
	private int storeOffset;

	/**
	 * Creates the page file on the file system.  Creates a file of zero length.
	 */
	public static void create(String fileName) throws CoreException {
		try {
			new java.io.File(fileName).createNewFile();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.createFailure"), e));//$NON-NLS-1$
		}
	}

	/**
	 * Deletes the page file from the file system.
	 */
	public static void delete(String fileName) {
		new File(fileName).delete();
	}

	/** 
	 * Returns true if the file exists in the file system.
	 */
	public static boolean exists(String fileName) {
		return new File(fileName).exists();
	}

	/**
	 * Creates a new PageStore with a given policy.
	 */
	public PageStore(AbstractPagePolicy policy) {
		this.policy = policy;
		this.storeOffset = NumberOfMetadataAreas * SizeOfMetadataArea;
	}

	//public void readFrom(RandomAccessFile file, long offset) throws IOException {
	//	long n = file.length() - offset;
	//	if (n <= 0) {
	//		clear(contents, 0, contents.length);
	//		return;
	//	}
	//	file.seek(offset);
	//	int m = (int)Math.min((long)contents.length, n);
	//	file.readFully(contents, 0, m);
	//	if (m < contents.length) {
	//		clear(contents, m, contents.length - m);
	//	}
	//}
	//public void writeTo(OutputStream out) throws IOException {
	//	out.write(contents);
	//}
	//public void writeTo(OutputStream out, int offset, int length) throws IOException {
	//	out.write(contents, offset, length);
	//}
	//public void writeTo(RandomAccessFile file, long offset) throws IOException {
	//	long p = file.length();
	//	long n = offset - p;
	//	while (n > 0) {
	//		int m = (int)Math.min((long)ZEROES.length, n);
	//		file.seek(p);
	//		file.write(ZEROES, 0, m);
	//		p += m;
	//		n -= m;
	//	}
	//	file.seek(offset);
	//	file.write(contents);
	//}

	/**
	 * Opens the PageStore with a cache size of 40.
	 */
	//public void open(String name) throws CoreException {
	//	open(name, 40);
	//}
	/**
	 * Opens the PageStore.  The file is created if necessary.
	 * This will raise an exception if the
	 * media on which the file is located is read-only 
	 * or not authorized to the user.
	 */
	//public void open(String name, int cacheSize) throws CoreException {
	//	if (!exists(name)) create(name);
	//	try {
	//		this.file = new RandomAccessFile(name, "rw");
	//	} catch (IOException e) {
	//		throw new CoreException(CoreException.OpenFailure);
	//	}
	//	this.name = name;
	//	checkMetadata();
	//	numberOfPages = numberOfPagesInFile();
	//	numberOfFileReads = 0;
	//	numberOfFileWrites = 0;
	//	numberOfReads = 0;
	//	numberOfWrites = 0;
	//	numberOfCacheHits = 0;
	//	/* apply any outstanding transaction by reading the log file and applying it */
	//	readCache = new PageCache(0);
	//	modifiedPages = LogReader.getModifiedPages(name);
	//	flush();
	//	Log.delete(name);
	//	/* prepare for normal operation */
	//	readCache = new PageCache(cacheSize);
	//	acquiredPages = new HashMap();
	//}
	/**
	 * Acquires the page that has the given page number from the page store.
	 */
	public Page acquire(int pageNumber) throws CoreException {
		numberOfReads++;
		Integer key = new Integer(pageNumber);
		Page page = (Page) acquiredPages.get(key);
		if (page == null) {
			page = (Page) modifiedPages.get(key);
			if (page == null) {
				numberOfPages = Math.max(pageNumber + 1, numberOfPages);
				page = readPage(pageNumber);
			} else {
				numberOfCacheHits++;
			}
			acquiredPages.put(key, page);
			page.addObserver(this);
		} else {
			numberOfCacheHits++;
		}
		page.addReference();
		return page;
	}

	/**
	 * Checks to see if the metadata stored in the page store matches that expected by this
	 * code.  If not, a conversion is necessary.
	 */
	private void checkMetadata() throws CoreException {
		byte[] md = readMetadataArea(0);
		Buffer metadata = new Buffer(md);
		Field versionField = metadata.getField(0, 4);
		int pageStoreVersion = versionField.getInt();
		if (pageStoreVersion == 0) {
			versionField.put(CurrentPageStoreVersion);
			writeMetadataArea(0, md);
			return;
		}
		if (pageStoreVersion != CurrentPageStoreVersion)
			throw Policy.exception("pageStore.conversionFailure");//$NON-NLS-1$
	}

	protected void clearFileToOffset(long fileOffset) {
		long fileLength = getFileLength();
		while (fileLength < fileOffset) {
			int m = (int) Math.min(ZEROES.length, (fileOffset - fileLength));
			writeBuffer(fileLength, ZEROES, 0, m);
			fileLength += m;
		}
	}

	/**
	 * Commits all changes and closes the page store.
	 */
	public void close() {
		close(true);
	}

	/**
	 * Closes the page store.
	 */
	public void close(boolean commit) {
		if (commit) {
			try {
				commit();
			} catch (CoreException e) {
				// ignore
			}
		}
		try {
			file.close();
		} catch (IOException e) {
			// ignore
		}
		file = null;
	}

	/**
	 * Commits all modified pages to the file.
	 */
	public void commit() throws CoreException {
		if (modifiedPages.size() == 0)
			return;
		LogWriter.putModifiedPages(this, modifiedPages);
		flush();
		Log.delete(name);
	}

	/**
	 * Writes the modified pages to the page file.
	 */
	private void flush() throws CoreException {
		if (modifiedPages.size() == 0)
			return;
		Iterator pageStream = modifiedPages.values().iterator();
		while (pageStream.hasNext()) {
			Page page = (Page) pageStream.next();
			writePage(page);
		}
		modifiedPages.clear();
	}

	protected long getFileLength() {
		long n = 0;
		try {
			n = file.length();
		} catch (IOException e) {
			return 0;
		}
		return n;
	}

	/**
	 * Returns the name of the page store.
	 */
	public String getName() {
		return name;
	}

	public AbstractPagePolicy getPolicy() {
		return policy;
	}

	/** 
	 * Returns the number of read cache hits that have been made on the cache.
	 */
	public int numberOfCacheHits() {
		return numberOfCacheHits;
	}

	/** 
	 * Returns the number of read operations that have been done to the underlying file.
	 */
	public int numberOfFileReads() {
		return numberOfFileReads;
	}

	/**
	 * Returns the number of write operations that have been done to the underlying file.
	 */
	public int numberOfFileWrites() {
		return numberOfFileWrites;
	}

	/**
	 * Returns the number of pages known about in the PageFile.  This can be greater than
	 * the number of pages actually in the underlying file in the file system if new ones
	 * have been manufactured and not yet written to the underlying file.
	 */
	public int numberOfPages() {
		return numberOfPages;
	}

	/**
	 * Returns the number of pages actually in the underlying file.
	 */
	protected int numberOfPagesInFile() {
		return (int) ((getFileLength() - offsetOfPage(0)) / Page.SIZE);
	}

	/** 
	 * Returns the number of read operations that have been done.
	 */
	public int numberOfReads() {
		return numberOfReads;
	}

	/**
	 * Returns the number of write operations that have been done.
	 */
	public int numberOfWrites() {
		return numberOfWrites;
	}

	/**
	 * Returns the file seek offset for a given metadata area
	 */
	protected long offsetOfMetadataArea(int i) {
		return (long) i * SizeOfMetadataArea;
	}

	/**
	 * Returns the file seek offset for a given page number.
	 */
	protected long offsetOfPage(int pageNumber) {
		return (long) (pageNumber * Page.SIZE) + storeOffset;
	}

	/**
	 * Opens the PageStore.  The file is created if necessary.
	 * This will raise an exception if the
	 * media on which the file is located is read-only 
	 * or not authorized to the user.
	 */
	public void open(String name) throws CoreException {
		this.name = name;
		pageBuffer = new byte[Page.SIZE];
		metadataBuffer = new byte[SizeOfMetadataArea];
		if (!exists(name))
			create(name);
		try {
			this.file = new RandomAccessFile(name, "rw"); //$NON-NLS-1$
		} catch (IOException e) {
			throw Policy.exception("pageStore.openFailure", e);//$NON-NLS-1$
		}
		checkMetadata();
		numberOfPages = numberOfPagesInFile();
		numberOfFileReads = 0;
		numberOfFileWrites = 0;
		numberOfReads = 0;
		numberOfWrites = 0;
		numberOfCacheHits = 0;
		/* apply any outstanding transaction by reading the log file and applying it */
		modifiedPages = LogReader.getModifiedPages(this);
		flush();
		Log.delete(name);
		/* prepare for normal operation */
		acquiredPages = new HashMap();
	}

	protected boolean readBuffer(long fileOffset, byte[] buffer) {
		new Buffer(buffer);
		long fileLength = getFileLength();
		if (fileOffset >= fileLength)
			return true;
		int bytesToRead = (int) Math.min(buffer.length, (fileLength - fileOffset));
		try {
			file.seek(fileOffset);
			file.readFully(buffer, 0, bytesToRead);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public byte[] readMetadataArea(int i) throws CoreException {
		if (!readBuffer(offsetOfMetadataArea(i), metadataBuffer))
			throw Policy.exception("pageStore.metadataRequestFailure");//$NON-NLS-1$
		return new Buffer(metadataBuffer).get(0, metadataBuffer.length);
	}

	protected Page readPage(int pageNumber) throws CoreException {
		if (!readBuffer(offsetOfPage(pageNumber), pageBuffer))
			throw Policy.exception("pageStore.readFailure");//$NON-NLS-1$
		numberOfFileReads++;
		Page p = policy.createPage(pageNumber, pageBuffer, this);
		p.addObserver(this);
		return p;
	}

	/**
	 * Releases a page and decrements its reference count.
	 */
	public void release(Page page) {
		Integer key = new Integer(page.getPageNumber());
		page.removeReference();
		if (page.hasReferences())
			return;
		page.deleteObserver(this);
		acquiredPages.remove(key);
	}

	/**
	 * Throws out the modified pages.
	 */
	public void rollback() {
		modifiedPages.clear();
	}

	/**
	 * Internal test for page log consistency.  Throws an exception if
	 * a problem is detected.
	 */
	public void testLogging1() throws CoreException {
		LogWriter.putModifiedPages(this, modifiedPages);
		Map testPages = LogReader.getModifiedPages(this);
		int m = testPages.size();
		int n = modifiedPages.size();
		if (m != n)
			throw Policy.exception(Integer.toString(m) + ' ' + n);//Page set sizes do not match
		Iterator testPagesStream = testPages.values().iterator();
		Iterator modifiedPagesStream = modifiedPages.values().iterator();
		while (testPagesStream.hasNext()) {
			Page testPage = (Page) testPagesStream.next();
			Page modifiedPage = (Page) modifiedPagesStream.next();
			//	Page number mismatch
			if (testPage.getPageNumber() != modifiedPage.getPageNumber())
				throw Policy.exception(Integer.toString(testPage.getPageNumber()) + ' ' + modifiedPage.getPageNumber());
			//	Page buffer mismatch
			if (Buffer.compare(testPage.pageBuffer, modifiedPage.pageBuffer) != 0)
				throw Policy.exception(Integer.toString(testPage.getPageNumber()));
		}
		Log.delete(name);
	}

	/**
	 * Internal test for applying a page log to the file.  Does the 
	 * equivalent of a flush.
	 */
	public void testLogging2() throws CoreException {
		LogWriter.putModifiedPages(this, modifiedPages);
		modifiedPages = LogReader.getModifiedPages(this);
		flush();
	}

	/**
	 * Internal test for simulating failure after the log is written but before the
	 * log is applied.  Tests the open sequence.  Does the equivalent of a close and
	 * open.  Pages must have been put to the store in order for this test to make sense.
	 * This should look like it does a flush, since the modified pages are written to the
	 * file.
	 */
	public void testLogging3() throws CoreException {
		LogWriter.putModifiedPages(this, modifiedPages);
		close(false);
		open(name);
	}

	/**
	 * Processes a page update.
	 */
	public void update(Observable object, Object arg) {
		Page page = (Page) object;
		Integer key = new Integer(page.getPageNumber());
		modifiedPages.put(key, page);
	}

	protected boolean writeBuffer(long fileOffset, byte[] buffer, int offset, int length) {
		clearFileToOffset(fileOffset);
		try {
			file.seek(fileOffset);
			file.write(buffer, offset, length);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public void writeMetadataArea(int i, byte[] buffer) throws CoreException {
		if (i < 0 || i >= NumberOfMetadataAreas || buffer.length != SizeOfMetadataArea)
			throw Policy.exception("pageStore.metadataRequestFailure");//$NON-NLS-1$
		if (!writeBuffer(offsetOfMetadataArea(i), buffer, 0, buffer.length))
			throw Policy.exception("pageStore.metadataRequestFailure");//$NON-NLS-1$
	}

	protected void writePage(Page page) throws CoreException {
		page.toBuffer(pageBuffer);
		long fileOffset = offsetOfPage(page.getPageNumber());
		if (!writeBuffer(fileOffset, pageBuffer, 0, pageBuffer.length))
			throw Policy.exception("pageStore.writeFailure");//$NON-NLS-1$
		numberOfFileWrites++;
	}
}