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
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;

class Log {

	/**
	 * Creates the log file in the file system.  The string 
	 * argument is the name of the page store for which this log will
	 * be created.
	 */
	static void create(String storeName) throws CoreException {
		try {
			new RandomAccessFile(name(storeName), "rw").close(); //$NON-NLS-1$
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, Policy.bind("pageStore.logCreateFailure"), e));//$NON-NLS-1$
		}
	}

	/**
	 * Deletes the transaction log from the file system.
	 */
	static void delete(String storeName) {
		new File(name(storeName)).delete();
	}

	/** 
	 * Returns true iff the transaction log exists in the file system.
	 */
	static boolean exists(String storeName) {
		return new File(name(storeName)).exists();
	}

	/**
	 * Returns the name of the log file, given the store name.
	 */
	static String name(String storeName) {
		return storeName + ".log"; //$NON-NLS-1$
	}
}