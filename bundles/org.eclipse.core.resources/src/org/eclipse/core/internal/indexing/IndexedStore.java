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

import java.util.*;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.runtime.CoreException;

public class IndexedStore {

	private static final int CurrentVersion = 1;
	private static final int MetadataID = 2;
	/*
	 * Provides the storage for the registry of stores. Key is the name the store
	 * was opened under.  The value is the store itself.  This is used to facilitate
	 * recovery in the event of a thread being killed or dying.
	 */
	private static final Map registry = Collections.synchronizedMap(new HashMap());

	private static final ObjectAddress ContextAddress10 = new ObjectAddress(1, 0);
	private static final ObjectAddress ContextAddress11 = new ObjectAddress(1, 1);

	private ObjectAddress objectDirectoryAddress; /* initialized at open */
	private Index objectDirectory; /* initialized at open */
	private IndexCursor objectDirectoryCursor; /* initialized at open */

	private ObjectAddress indexDirectoryAddress; /* initialized at open */
	private Index indexDirectory; /* initialized at open */
	private IndexCursor indexDirectoryCursor; /* initialized at open */
	private ObjectAddress contextAddress;

	private ObjectStore objectStore; /* initialized at open */
	private String name; /* initialized at open */

	/**
	 * Acquires an anchor.
	 */
	IndexAnchor acquireAnchor(ObjectAddress address) throws CoreException {
		return (IndexAnchor) acquireObject(address);
	}

	/**
	 * Acquires a context.  Returns null if the context could not be acquired.
	 */
	IndexedStoreContext acquireContext(ObjectAddress address) {
		try {
			return (IndexedStoreContext) acquireObject(address);
		} catch (CoreException e) {
			//context couldn't be acquired - return null
			return null;
		}
	}

	/**
	 * Acquire an index node.
	 */
	IndexNode acquireNode(ObjectAddress address) throws CoreException {
		return (IndexNode) acquireObject(address);
	}

	/**
	 * Acquires an object.
	 */
	private StoredObject acquireObject(ObjectAddress address) throws CoreException {
		StoredObject object;
		try {
			object = objectStore.acquireObject(address);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotAcquired", e); //$NON-NLS-1$
		}
		return object;
	}

	/**
	 * Acquires a Binary Object.
	 */
	BinarySmallObject acquireBinarySmallObject(ObjectAddress address) throws CoreException {
		return (BinarySmallObject) acquireObject(address);
	}

	/**
	 * Checks to see if the metadata stored in the object store matches that expected by this
	 * code.  If not, a conversion is necessary.
	 */
	private void checkMetadata() throws CoreException {
		Buffer metadata = getMetadataArea(MetadataID);
		Field versionField = metadata.getField(0, 4);
		int version = versionField.getInt();
		if (version == 0) {
			// 0 indicates that the store is new
			versionField.put(CurrentVersion);
			putMetadataArea(MetadataID, metadata);
			return;
		}
		if (version == CurrentVersion)
			return;
		convert(version);
	}

	/**
	 * Closes the store.  This is required to free the underlying file.
	 */
	public synchronized void close() throws CoreException {
		if (name == null)
			return;//already closed
		try {
			commit();
			if (objectDirectoryCursor != null)
				objectDirectoryCursor.close();
			if (indexDirectoryCursor != null)
				indexDirectoryCursor.close();
		} catch (CoreException e) {
			//make sure the file gets closed no matter what
			try {
				objectStore.close();
			} catch (CoreException e2) {
				//ignore this and rethrow the underlying exception
			}
			throw e;
		}
		try {
			objectStore.close();
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.storeNotClosed", e); //$NON-NLS-1$
		}
		registry.remove(name);
		name = null;
		objectDirectory = null;
		objectDirectoryAddress = null;
		objectDirectoryCursor = null;
		indexDirectory = null;
		indexDirectoryAddress = null;
		indexDirectoryCursor = null;
	}

	public synchronized void commit() throws CoreException {
		try {
			objectStore.commit();
		} catch (Exception e) {
			throw Policy.exception("indexedStore.storeNotCommitted", e); //$NON-NLS-1$
		}
	}

	/**
	 * Converts the store from a previous to the current version.  
	 * No conversions are yet defined.
	 */
	private void convert(int fromVersion) throws CoreException {
		throw Policy.exception("indexedStore.storeNotConverted"); //$NON-NLS-1$
	}

	/**
	 * Creates and initializes an IndexedStore.
	 */
	public static synchronized void create(String name) throws CoreException {
		ObjectStore store = new ObjectStore(new IndexedStoreObjectPolicy());
		try {
			ObjectStore.create(name);
			store.open(name);
			ObjectAddress contextAddress = store.insertObject(new IndexedStoreContext());
			IndexedStoreContext context = (IndexedStoreContext) store.acquireObject(contextAddress);
			IndexAnchor anchor = new IndexAnchor();
			ObjectAddress address = store.insertObject(anchor);
			context.setIndexDirectoryAddress(address);
			anchor = new IndexAnchor();
			address = store.insertObject(anchor);
			context.setObjectDirectoryAddress(address);
			context.release();
			store.commit();
			store.close();
		} catch (Exception e1) {
			try {
				store.close();
			} catch (CoreException e2) {
				//real exception thrown below
			}
			ObjectStore.delete(name);
			throw Policy.exception("indexedStore.storeNotCreated", e1); //$NON-NLS-1$
		}
	}

	/**
	 * Creates an Index with the given name.
	 */
	public synchronized Index createIndex(String indexName) throws CoreException {
		Index index = null;
		indexDirectoryCursor.find(indexName);
		if (indexDirectoryCursor.keyMatches(indexName)) {
			throw Policy.exception("indexedStore.indexExists"); //$NON-NLS-1$
		}
		ObjectAddress address = insertObject(new IndexAnchor());
		indexDirectory.insert(indexName, address.toByteArray());
		index = new Index(this, address);
		return index;
	}

	/**
	 * Places a byte array into the store, return a new object identifier.
	 */
	public synchronized ObjectID createObject(byte[] b) throws CoreException {
		ObjectAddress address = insertObject(new BinarySmallObject(b));
		ObjectID id = getNextObjectID();
		objectDirectory.insert(id.toByteArray(), address.toByteArray());
		return id;
	}

	/**
	 * Places a String into the store.
	 */
	public synchronized ObjectID createObject(String s) throws CoreException {
		return createObject(Convert.toUTF8(s));
	}

	/**
	 * Places an Insertable into the store.
	 */
	public synchronized ObjectID createObject(Insertable anObject) throws CoreException {
		return createObject(anObject.toByteArray());
	}

	/**
	 * Deletes the store if it exists.  Does nothing if it does not exist.
	 */
	public static synchronized void delete(String filename) {
		ObjectStore.delete(filename);
	}

	/**
	 * Tests to see if the file acting as the store exists.
	 */
	public static synchronized boolean exists(String filename) {
		return ObjectStore.exists(filename);
	}

	/**
	 * If a store disappears unexpectedly, make sure it gets closed.
	 */
	protected void finalize() {
		try {
			close();
		} catch (Exception e) {
			//unsafe to throw exceptions from a finalize
		}
	}

	/**
	 * Finds the handle of an open store for a given its name.  The store may continue with the current transaction,
	 * or may abort the current transaction.  Used to initiate recovery if the reference to the store should be
	 * lost for some reason.  Will return null if the store has not been opened.  The name of the store to be found
	 * must compare equal to the name the store was opened under.
	 */
	public synchronized static IndexedStore find(String name) {
		return (IndexedStore) registry.get(name);
	}

	/**
	 * @deprecated -- use commit()
	 */
	public synchronized void flush() throws CoreException {
		try {
			objectStore.commit();
		} catch (Exception e) {
			throw Policy.exception("indexedStore.storeNotFlushed", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns an index given its name, or null if no matching index was found
	 */
	public synchronized Index getIndex(String indexName) throws CoreException {
		Index index;
		byte[] key = Convert.toUTF8(indexName);
		indexDirectoryCursor.find(key);
		if (!indexDirectoryCursor.keyMatches(key))
			return null;
		ObjectAddress address = indexDirectoryCursor.getValueAsObjectAddress();
		index = new Index(this, address);
		return index;
	}

	private Buffer getMetadataArea(int i) throws CoreException {
		try {
			return objectStore.getMetadataArea(i);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.metadataRequestError", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the name of the store.
	 */
	public synchronized String getName() {
		return name;
	}

	/**
	 * Returns the next ObjectID
	 */
	private ObjectID getNextObjectID() throws CoreException {
		IndexedStoreContext context = acquireContext(contextAddress);
		if (context == null)
			throw Policy.exception("indexedStore.contextNotAvailable"); //$NON-NLS-1$
		long objectNumber = context.getNextObjectNumber();
		context.release();
		return new ObjectID(objectNumber);
	}

	/**
	 * Returns a byte array given its object identifier.
	 */
	public synchronized byte[] getObject(ObjectID id) throws CoreException {
		objectDirectoryCursor.find(id.toByteArray());
		ObjectAddress address = objectDirectoryCursor.getValueAsObjectAddress();
		BinarySmallObject object = acquireBinarySmallObject(address);
		byte[] b = object.getValue();
		object.release();
		return b;
	}

	/**
	 * Returns an object as a string, truncated at the first null.
	 */
	public synchronized String getObjectAsString(ObjectID id) throws CoreException {
		String s;
		s = Convert.fromUTF8(getObject(id));
		int i = s.indexOf(0);
		if (i == -1)
			return s;
		return s.substring(0, i);
	}

	/**
	 * Returns the object store.
	 */
	public synchronized ObjectStore getObjectStore() {
		return objectStore;
	}

	/** 
	 * Inserts a new object into my store.
	 */
	ObjectAddress insertObject(StoredObject object) throws CoreException {
		try {
			ObjectAddress address = objectStore.insertObject(object);
			return address;
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotStored", e); //$NON-NLS-1$
		}
	}

	/**
	 * Opens the store.
	 */
	public synchronized void open(String name) throws CoreException {
		if (registry.get(name) != null) {
			throw Policy.exception("indexedStore.storeIsOpen"); //$NON-NLS-1$
		}
		if (!exists(name))
			create(name);
		objectStore = new ObjectStore(new IndexedStoreObjectPolicy());
		objectStore.open(name);
		checkMetadata();
		contextAddress = ContextAddress10;
		IndexedStoreContext context = acquireContext(contextAddress);
		if (context == null) {
			contextAddress = ContextAddress11;
			context = acquireContext(contextAddress);
		}
		if (context == null) {
			throw Policy.exception("indexedStore.storeFormatError"); //$NON-NLS-1$
		}
		indexDirectoryAddress = context.getIndexDirectoryAddress();
		objectDirectoryAddress = context.getObjectDirectoryAddress();
		context.release();
		indexDirectory = new Index(this, indexDirectoryAddress);
		indexDirectoryCursor = indexDirectory.open();
		objectDirectory = new Index(this, objectDirectoryAddress);
		objectDirectoryCursor = objectDirectory.open();
		this.name = name;
		registry.put(name, this);
	}

	private void putMetadataArea(int i, Buffer b) throws CoreException {
		try {
			objectStore.putMetadataArea(i, b);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.metadataRequestError", e); //$NON-NLS-1$
		}
	}

	/**
	 * Destroys an Index given its name.
	 */
	public synchronized void removeIndex(String indexName) throws CoreException {
		byte[] key = Convert.toUTF8(indexName);
		indexDirectoryCursor.find(key);
		if (!indexDirectoryCursor.keyMatches(key)) {
			throw Policy.exception("indexedStore.indexNotFound"); //$NON-NLS-1$
		}
		ObjectAddress address = indexDirectoryCursor.getValueAsObjectAddress();
		IndexAnchor anchor = acquireAnchor(address);
		anchor.destroyChildren();
		anchor.release();
		removeObject(address);
		indexDirectoryCursor.remove();
	}

	/** 
	 * Removes an object from my store.
	 */
	void removeObject(ObjectAddress address) throws CoreException {
		try {
			objectStore.removeObject(address);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotRemoved", e); //$NON-NLS-1$
		}
	}

	/**
	 * Removes the object identified by id from the store.
	 */
	public synchronized void removeObject(ObjectID id) throws CoreException {
		byte[] key = id.toByteArray();
		objectDirectoryCursor.find(key);
		if (!objectDirectoryCursor.keyMatches(key)) {
			Policy.exception("indexedStore.objectNotFound"); //$NON-NLS-1$
		}
		ObjectAddress address = objectDirectoryCursor.getValueAsObjectAddress();
		objectDirectoryCursor.remove();
		removeObject(address);
	}

	public synchronized void rollback() {
		objectStore.rollback();
	}

	/**
	 * Replaces the contents of the object identified by "id" with the byte array "b".
	 */
	public synchronized void updateObject(ObjectID id, byte[] b) throws CoreException {
		byte[] key = id.toByteArray();
		objectDirectoryCursor.find(key);
		if (!objectDirectoryCursor.keyMatches(key)) {
			Policy.exception("indexedStore.objectNotFound"); //$NON-NLS-1$
		}
		ObjectAddress oldAddress = objectDirectoryCursor.getValueAsObjectAddress();
		ObjectAddress newAddress = insertObject(new BinarySmallObject(b));
		objectDirectoryCursor.updateValue(newAddress.toByteArray());
		removeObject(oldAddress);
	}

	/**
	 * Updates an object with a String.
	 */
	public synchronized void updateObject(ObjectID id, String s) throws CoreException {
		updateObject(id, Convert.toUTF8(s));
	}

	/**
	 * Updates an object with an Insertable.
	 */
	public synchronized void updateObject(ObjectID id, Insertable anObject) throws CoreException {
		updateObject(id, anObject.toByteArray());
	}
}