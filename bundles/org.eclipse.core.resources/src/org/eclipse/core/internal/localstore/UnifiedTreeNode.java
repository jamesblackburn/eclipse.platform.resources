/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.util.Iterator;
import org.eclipse.core.filesystem.FileStore;
import org.eclipse.core.resources.IResource;

public class UnifiedTreeNode implements ILocalStoreConstants {
	protected IResource resource;
	protected UnifiedTreeNode child;
	protected UnifiedTree tree;
	protected FileStore store;
	protected boolean existsWorkspace;

	//the location of the resource in the local file system, if any
	protected String localName;

	public UnifiedTreeNode(UnifiedTree tree, IResource resource, FileStore store, String localName, boolean existsWorkspace) {
		this.tree = tree;
		this.resource = resource;
		this.store = store;
		this.existsWorkspace = existsWorkspace;
		this.localName = localName;
	}

	public boolean existsInFileSystem() {
		return isFile() || isFolder();
	}

	public boolean existsInWorkspace() {
		return existsWorkspace;
	}

	/**
	 * Returns an Enumeration of UnifiedResourceNode.
	 */
	public Iterator getChildren() {
		return tree.getChildren(this);
	}

	protected UnifiedTreeNode getFirstChild() {
		return child;
	}

	public long getLastModified() {
		return store.lastModified();
	}

	public int getLevel() {
		return tree.getLevel();
	}

	/**
	 * Returns the local store of this resource.  May be null.
	 */
	public FileStore getStore() {
		return store;
	}

	/**
	 * Gets the name of this node in the local file system.
	 * @return Returns a String
	 */
	public String getLocalName() {
		return localName;
	}

	public IResource getResource() {
		return resource;
	}

	public boolean isFile() {
		return !store.isDirectory();
	}

	public boolean isFolder() {
		return store.isDirectory();
	}

	public void setExistsWorkspace(boolean exists) {
		this.existsWorkspace = exists;
	}

	protected void setFirstChild(UnifiedTreeNode child) {
		this.child = child;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

	public String toString() {
		String s = resource == null ? "null" : resource.getFullPath().toString(); //$NON-NLS-1$
		return "Node: " + s; //$NON-NLS-1$
	}

	public void removeChildrenFromTree() {
		tree.removeNodeChildrenFromQueue(this);
	}

	/**
	 * Reuses this object by assigning all new values for the fields.
	 */
	public void reuse(UnifiedTree aTree, IResource aResource, FileStore aStore, String aLocalName, boolean existsInWorkspace) {
		this.tree = aTree;
		this.child = null;
		this.resource = aResource;
		this.store = aStore;
		this.existsWorkspace = existsInWorkspace;
		this.localName = aLocalName;
	}
}
