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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

class LogWriter {

	protected FileOutputStream out;
	protected PageStore pageStore;

	/**
	 * Puts the modified pages to the log file.
	 */
	public static void putModifiedPages(PageStore pageStore, Map modifiedPages) throws CoreException {
		LogWriter writer = new LogWriter();
		try {
			writer.open(pageStore);
			writer.putModifiedPages(modifiedPages);
		} finally {
			writer.close();
		}
	}

	/**
	 * Opens the log.
	 */
	protected void open(PageStore store) throws CoreException {
		this.pageStore = store;
		try {
			out = new FileOutputStream(Log.name(store.getName()));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logOpenFailure"), e));//$NON-NLS-1$
		}
	}

	/**
	 * Closes the log.
	 */
	protected void close() {
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			// ignore
		}
		out = null;
	}

	/**
	 * Puts the modified pages into the log.
	 */
	protected void putModifiedPages(Map modifiedPages) throws CoreException {
		Buffer b4 = new Buffer(4);
		byte[] pageBuffer = new byte[Page.SIZE];
		int numberOfPages = modifiedPages.size();
		b4.put(0, 4, numberOfPages);
		try {
			write(b4.getByteArray());
			Iterator pageStream = modifiedPages.values().iterator();
			while (pageStream.hasNext()) {
				Page page = (Page) pageStream.next();
				int pageNumber = page.getPageNumber();
				b4.put(0, 4, pageNumber);
				write(b4.getByteArray());
				page.toBuffer(pageBuffer);
				write(pageBuffer);
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logWriteFailure"), e));//$NON-NLS-1$
		}
	}

	public void write(byte[] buffer) throws IOException {
		out.write(buffer);
	}

}