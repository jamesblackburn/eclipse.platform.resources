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

import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.runtime.CoreException;

abstract class IndexedStoreObject extends StoredObject {

	public IndexedStoreObject() {
		super();
	}

	/**
	 * Constructs an object from bytes that came from the store.
	 * These bytes include the 2 byte type field.
	 */
	public IndexedStoreObject(Field f, ObjectStore store, ObjectAddress address) throws CoreException {
		super(f, store, address);
	}

	/**
	 * Acquires an anchor.
	 */
	protected final IndexAnchor acquireAnchor(ObjectAddress address) throws CoreException {
		return (IndexAnchor) acquireObject(address);
	}

	/**
	 * Acquires a node.
	 */
	protected final IndexNode acquireNode(ObjectAddress address) throws CoreException {
		return (IndexNode) acquireObject(address);
	}

	/**
	 * Acquires an object.
	 */
	protected final StoredObject acquireObject(ObjectAddress address) throws CoreException {
		StoredObject object;
		try {
			object = store.acquireObject(address);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotAcquired", e); //$NON-NLS-1$
		}
		return object;
	}

	/** 
	 * Inserts a new object into my store. Subclasses must not override.
	 */
	protected final ObjectAddress insertObject(StoredObject object) throws CoreException {
		try {
			ObjectAddress address = store.insertObject(object);
			return address;
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotStored", e); //$NON-NLS-1$
		}
	}

	/**
	 * Releases this object.  Subclasses must not override.
	 */
	protected final void release() {
		store.releaseObject(this);
	}

	/** 
	 * Removes an object from my store.  Subclasses must not override.
	 */
	protected final void removeObject(ObjectAddress address) throws CoreException {
		try {
			store.removeObject(address);
		} catch (CoreException e) {
			throw Policy.exception("indexedStore.objectNotRemoved", e); //$NON-NLS-1$
		}
	}
}