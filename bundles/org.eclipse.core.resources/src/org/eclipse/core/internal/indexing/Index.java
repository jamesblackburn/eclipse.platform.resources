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

import java.util.Vector;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.runtime.CoreException;

/**
 * This class provides the public interface to an index.
 */
public class Index {

	private IndexedStore store;
	private ObjectAddress anchorAddress;

	/**
	 * Default Index constructor.
	 */
	private Index() {
		super();
	}

	/**
	 * Index constructor.
	 */
	Index(IndexedStore store, ObjectAddress anchorAddress) {
		this.store = store;
		this.anchorAddress = anchorAddress;
	}

	/**
	 * Returns the number of entries in the index.
	 */
	public synchronized int getNumberOfEntries() throws CoreException {
		IndexAnchor anchor = store.acquireAnchor(anchorAddress);
		int n = anchor.getNumberOfEntries();
		anchor.release();
		return n;
	}

	/**
	 * Returns the number of nodes in the index.
	 */
	public synchronized int getNumberOfNodes() throws CoreException {
		IndexAnchor anchor = store.acquireAnchor(anchorAddress);
		int n = anchor.getNumberOfNodes();
		anchor.release();
		return n;
	}

	/**
	 * Returns a vector of ObjectIDs whose keys match the key given in the index.  
	 * This assumes that the underlying index has values that can be converted 
	 * to ObjectIDs.
	 */
	public synchronized Vector getObjectIdentifiersMatching(byte[] key) throws CoreException {
		IndexCursor cursor = open();
		cursor.find(key);
		Vector vector = new Vector(20);
		while (cursor.keyMatches(key)) {
			vector.addElement(cursor.getValueAsObjectID());
			cursor.next();
		}
		cursor.close();
		return vector;
	}

	/**
	 * Returns a vector of ObjectIDs whose keys match the
	 * key given in the index.  This assumes that the underlying 
	 * index has values that can be converted to ObjectIDs.
	 */
	public synchronized Vector getObjectIdentifiersMatching(String key) throws CoreException {
		return getObjectIdentifiersMatching(Convert.toUTF8(key));
	}

	/**
	 * Returns a vector of ObjectIDs whose keys match the
	 * key given in the index.  This assumes that the underlying 
	 * index has values that can be converted to ObjectIDs.
	 */
	public synchronized Vector getObjectIdentifiersMatching(Insertable key) throws CoreException {
		return getObjectIdentifiersMatching(key.toByteArray());
	}

	/**
	 * Inserts an entry into an index.  The key and the value are byte arrays.  
	 * Keys cannot be more than 1024 bytes in length.  Values must not 
	 * be greater than 2048 bytes in length.  The other insert methods are 
	 * convenience methods that use this for their implementation.
	 */
	public synchronized void insert(byte[] key, byte[] value) throws CoreException {
		if (key.length > 1024)
			throw Policy.exception("indexedStore.entryKeyLengthError"); //$NON-NLS-1$
		if (value.length > 2048)
			throw Policy.exception("indexedStore.entryValueLengthError"); //$NON-NLS-1$
		IndexAnchor anchor = store.acquireAnchor(anchorAddress);
		anchor.insert(key, value);
		anchor.release();
	}

	public synchronized void insert(byte[] key, String value) throws CoreException {
		insert(key, Convert.toUTF8(value));
	}

	public synchronized void insert(byte[] key, Insertable value) throws CoreException {
		insert(key, value.toByteArray());
	}

	public synchronized void insert(String key, byte[] value) throws CoreException {
		insert(Convert.toUTF8(key), value);
	}

	public synchronized void insert(String key, String value) throws CoreException {
		insert(Convert.toUTF8(key), Convert.toUTF8(value));
	}

	public synchronized void insert(String key, Insertable value) throws CoreException {
		insert(Convert.toUTF8(key), value.toByteArray());
	}

	public synchronized void insert(Insertable key, byte[] value) throws CoreException {
		insert(key.toByteArray(), value);
	}

	public synchronized void insert(Insertable key, String value) throws CoreException {
		insert(key.toByteArray(), Convert.toUTF8(value));
	}

	public synchronized void insert(Insertable key, Insertable value) throws CoreException {
		insert(key.toByteArray(), value.toByteArray());
	}

	/**
	 * Returns a cursor for this index.  The cursor is initially in the unset state
	 * and should be positioned using "find" before being used.
	 */
	public synchronized IndexCursor open() {
		IndexCursor c = new IndexCursor(store, anchorAddress);
		return c;
	}

	/**
	 * Removes all entries that have a key that is equal to the supplied key.
	 */
	public synchronized void removeAllEqual(byte[] key) throws CoreException {
		IndexCursor c = open();
		c.find(key);
		while (c.keyEquals(key)) {
			c.removeEntry();
		}
		c.close();
	}

	/**
	 * Removes all entries that have a key that begins with the supplied prefix.
	 */
	public synchronized void removeAllMatching(byte[] keyPrefix) throws CoreException {
		IndexCursor c = open();
		c.find(keyPrefix);
		while (c.keyMatches(keyPrefix)) {
			c.removeEntry();
		}
		c.close();
	}
}