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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

class LogReader {
	protected byte[] b4;

	protected FileInputStream in;
	protected byte[] pageBuffer;
	protected PageStore store;

	/** 
	 * Returns the Hashmap of the modified pages.
	 */
	public static Map getModifiedPages(PageStore store) throws CoreException {
		LogReader reader = new LogReader(store);
		Map modifiedPages = null;
		try {
			reader.open(store);
			modifiedPages = reader.getModifiedPages();
		} finally {
			reader.close();
		}
		return modifiedPages;
	}

	public LogReader(PageStore store) {
		this.store = store;
		this.pageBuffer = new byte[Page.SIZE];
		this.b4 = new byte[4];
	}

	protected int bytesAvailable() throws CoreException {
		try {
			return in.available();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logReadFailure"), e));//$NON-NLS-1$
		}
	}

	/**
	 * Closes the log.
	 */
	protected void close() {
		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
			// ignore
		}
		in = null;
	}

	/**
	 * Returns the Hashmap of modified pages read from the log.
	 */
	protected Map getModifiedPages() throws CoreException {
		Map modifiedPages = new TreeMap();
		if (in == null)
			return modifiedPages;
		Field f4 = new Field(b4);
		readBuffer(b4);
		int numberOfPages = f4.getInt();
		int recordSize = 4 + Page.SIZE;
		if (bytesAvailable() != (numberOfPages * recordSize))
			return modifiedPages;
		for (int i = 0; i < numberOfPages; i++) {
			readBuffer(b4);
			readBuffer(pageBuffer);
			int pageNumber = f4.getInt();
			Page page = store.getPolicy().createPage(pageNumber, pageBuffer, store);
			Integer key = new Integer(pageNumber);
			modifiedPages.put(key, page);
		}
		return modifiedPages;
	}

	/** 
	 * Open a log for reading.
	 */
	protected void open(PageStore pageStore) throws CoreException {
		String name = pageStore.getName();
		if (!Log.exists(name))
			return;
		try {
			in = new FileInputStream(Log.name(name));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logOpenFailure"), e));//$NON-NLS-1$
		}
	}

	public void readBuffer(byte[] buffer) throws CoreException {
		try {
			in.read(buffer);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logReadFailure"), e));//$NON-NLS-1$
		}
	}

}