package org.eclipse.core.resources;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import org.eclipse.core.runtime.IPath;

/**
 * Mappings allow projects to contain resources which are mapped to
 * disparate locations in the local file system.
 * All resources below a project can have their content
 * stored in a specified location using a default mapping.
 * Additionally, resources which are immediate children of a project
 * can have their content stored in a specific location using 
 * mappings bearing the name of the child.
 * <p>
 * The project's mappings determine the location in the local file system 
 * for all its resources.
 * A project's various mappings must not overlap each other.
 * If there are also specific mappings, they take precedence over the default
 * mapping. 
 * The location of a mapping may be <code>null</code>. 
 * When a mapping with a <code>null</code>
 * location is used in a project, the location which is 
 * <code>null</code> is ignored and the project's corresponding default
 * location is used.
 * There is always a default mapping.
 * If the location of the default mapping is <code>null</code>,
 * then this resource maps to the standard default location for the
 * project content area within the workspace (this file system path is
 * not encoded in the mapping because it would have to change when
 * the project resource was renamed).
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
 * For these reasons, it is recommended that mappings only be changed
 * when the project is closed.
 * </p>
 *
 * @see IResource#PROJECT_ROOT
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
 * Returns the name of the resource being mapped, or the special value
 * <code>PROJECT_ROOT</code> for the default mapping.
 *
 * @return the name of the resource being mapped
 * @see #setName
 * @see IResource#PROJECT_ROOT
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
 * Sets the name of the resource being mapped to the given name,
 * or to the special value indicating that this is the default mapping.
 * <p>
 * This is the name of a resource that would appear as an immediate
 * member of the project in the workspace.  That is, the name must
 * be a valid path segment.
 * The one exception to this is the the default mapping,
 * whose name is the value of the <code>IResource.PROJECT_ROOT</code> constant;
 * there would not be any resource in the workspace with this name.
 * </p>
 *
 * @param name the name of the resource to be mapped, or 
 *	  <code>IResource.PROJECT_ROOT</code> for the default mapping
 * @see #getName
 * @see IPath#isValidSegment
 * @see IResource#PROJECT_ROOT
 */
public void setName(String name);
}
