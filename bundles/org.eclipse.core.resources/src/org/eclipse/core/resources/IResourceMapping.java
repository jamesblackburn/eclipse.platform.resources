/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.resources;

import org.eclipse.core.runtime.IPath;

/**
 * Mappings allow projects to contain resources which are mapped to
 * disparate locations in the local file system.
 * All resources below a project can have their content
 * stored in a specified location by setting the project location.
 * Additionally, resources which are immediate children of a project
 * can have their content stored in a specific location using 
 * mappings bearing the name of the child.
 * <p>
 * The project's location and mappings determine the location in the local file system 
 * for all its resources.
 * A project's various mappings must not overlap each other.
 * If there are specific mappings, they take precedence over the project location.
 * The location of a mapping may be <code>null</code>. 
 * When a mapping with a <code>null</code>
 * location is used in a project, the location which is 
 * <code>null</code> is ignored and the project's corresponding default
 * location is used.
 * </p>
 * <p>
 * Mappings involve an absolute path into the local file system;
 * that is, a file system path meaningful to the machine running the workspace.
 * </p>
 * <p>
 * Mappings can be changed at any time. If the project is open,
 * the mapping changes will induce apparent changes to the project's
 * resources which will require resynchronization.
 * For example, changing the mapping for a folder
 * will probably induce to the workspace as a large number of resource 
 * creations and deletions (and changes where file names happen to overlap).
 * </p>
 *
 * @see IProject#newMapping
 * @see IProject#getMappings
 * @see IProject#getMapping
 * @see IProject#addMapping
 * @see IProject#removeMapping
 */

public interface IResourceMapping extends Cloneable {
/**
 * Returns the local file system mapping,
 * or <code>null</code> if none.
 *
 * @return an absolute local file system path, or <code>null</code>
 * @see #setLocation
 */
public IPath getLocation();
/**
 * Returns the name of the resource being mapped
 *
 * @return the name of the resource being mapped
 * @see #setName
 */
public String getName();
/**
 * Sets the local file system mapping to the given 
 * path, which may be <code>null</code>.
 *
 * @param location an absolute local file system path, or <code>null</code>
 * @see #getLocation
 */
public void setLocation(IPath location);
/**
 * Sets the name of the resource being mapped to the given name.
 * <p>
 * This is the name of a resource that would appear as an immediate
 * member of the project in the workspace.  That is, the name must
 * be a valid path segment.
 * </p>
 *
 * @param name the name of the resource to be mapped
 * @see #getName
 * @see IPath#isValidSegment
 * @see IResource#PROJECT_ROOT
 */
public void setName(String name);
}
