/**********************************************************************
 * Copyright (c) 2000,2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.resources;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * A project description contains the metadata required to define
 * a project.  In effect, a project description is a project's "content".
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface IProjectDescription {
	/**
	 * Constant name of the project description file (value <code>".project"</code>). 
	 * The handle of a project's description file is 
	 * <code>project.getFile(DESCRIPTION_FILE_NAME)</code>.
	 * The project description file is located in the root of the project's content area.
	 * 
	 * @return the filename for the project description
	 * @since 2.0
	 */
	public static final String DESCRIPTION_FILE_NAME = ".project"; //$NON-NLS-1$
/**
 * Adds the given resource mapping to this project description, replacing 
 * an existing mapping of the same name.
 * This project need not be open.
 * <p>
 * Changing resource mappings does not directly modify any resources 
 * in the project; it simply changes the mapping defining where those
 * resources are stored. However, changing a project's mappings while
 * it is open should be expected to cause the workspace to become out
 * of sync with the local file system.
 * </p>
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param mapping the mapping to add
 *
 * @see IResourceMapping
 * @see #newMapping
 * @see #getMapping
 * @see #removeMapping
 */
public void addMapping(IResourceMapping mapping);
/**
 * Returns the list of build commands to run when building the described project.
 * The commands are listed in the order in which they are to be run.
 *
 * @return the list of build commands for the described project 
 */
public ICommand[] getBuildSpec();
/**
 * Returns the descriptive comment for the described project.
 *
 * @return the comment for the described project
 */
public String getComment();
/**
 * Returns the  local file system location for the described project.
 * <code>null</code> is returned if the default location should be used.
 *
 * @return the location for the described project or <code>null</code>
 */
public IPath getLocation();
/**
 * Returns this project's mapping for the resource with the given name, or
 * <code>null</code> if it does not have one. If the name is the value of the
 * <code>PROJECT_ROOT</code> constant, this project's default mapping is
 * returned (it always has one).
 * This project need not be open.
 * <p>
 * The mapping returned is a copy which can be edited.
 * Editing the returned value does not change the mappings for this project.
 * The changed mapping must be re-installed using the <code>addMapping</code> method.
 * </p>
 *
 * @param name the name of the resource to be mapped, or <code>PROJECT_ROOT</code>
 *    for the default mapping
 * @return the mapping for the named resource, or <code>null</code> 
 *     if it does not have one. 
 * @see IResourceMapping
 * @see IResource#PROJECT_ROOT
 * @see #newMapping
 * @see #getMappings
 * @see #addMapping
 * @see #removeMapping
 */
public IResourceMapping getMapping(String name);
/**
 * Returns a table of all the mappings defined for this project.
 * This project need not be open.
 * <p>
 * The keys are names (<code>String</code>s); the values are
 * mappings (<code>IResourceMapping</code>s).
 * The mappings returned are copies which may be edited.
 * Editing the returned value, or the mappings it contains,
 * does not change the mappings for this project.
 * Changed mappings must be re-installed using the 
 * <code>addMapping</code> method.
 * </p>
 *
 * @return the table of resource mappings, keyed by name
 *  (key type: <code>String</code>, value type: <code>IResourceMapping</code>) 
 * @see IResourceMapping
 * @see #newMapping
 * @see #getMapping
 * @see #addMapping
 * @see #removeMapping
 */
public Map getMappings() throws CoreException;
/**
 * Returns the name of the described project.
 *
 * @return the name of the described project
 */
public String getName();
/** 
 * Returns the list of natures associated with the described project.
 * Returns an empty array if there are no natures on this description.
 *
 * @return the list of natures for the described project
 * @see #setNatureIds
 */ 
public String[] getNatureIds();
/**
 * Returns the projects referenced by the described project.
 * The projects need not exist in the workspace.
 * The result will not contain duplicates. Returns an empty
 * array if there are no referenced projects on this description.
 *
 * @return a list of projects
 */
public IProject[] getReferencedProjects();
/** 
 * Returns whether the project nature specified by the given
 * nature extension id has been added to the described project. 
 *
 * @param natureId the nature extension identifier
 * @return <code>true</code> if the described project has the given nature 
 */
public boolean hasNature(String natureId);
/**
 * Returns a new build command.
 * <p>
 * Note that the new command does not become part of this project
 * description's build spec until it is installed via the <code>setBuildSpec</code>
 * method.
 * </p>
 *
 * @return a new command
 * @see #setBuildSpec
 */
public ICommand newCommand();
/**
 * Creates and returns a new mapping with the given attributes.
 * <p>
 * The returned value can be added to this project description using
 * the <code>addMapping</code> method.
 *
 * @param name the name of the resource to be mapped, or <code>PROJECT_ROOT</code>
 *    for the default mapping
 * @param local an absolute local file system path, or <code>null</code>
 * @return a new resource mapping
 * @see IResourceMapping
 * @see IResource#PROJECT_ROOT
 * @see #getMappings
 * @see #addMapping
 * @see #removeMapping
 */
public IResourceMapping newMapping(String name, IPath local);
/**
 * Removes the mapping with the given name from this project description.
 * If the resource has no such mapping, no action is taken. 
 * If the name is the value of the <code>PROJECT_ROOT</code> constant, 
 * this project's default mapping is reset to its initial value.
 * This project need not be open.
 * <p>
 * Changing resource mappings does not directly modify any resources 
 * in the project; it simply changes the mapping defining where those
 * resources are stored. However, changing a project's mappings while
 * it is open should be expected to cause the workspace to become out
 * of sync with the local file system.
 * </p>
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param name the string name of the mapping to remove
 * @exception CoreException if this method fails. Reasons include:
 * <ul>
 * <li> This project does not exist.</li>
 * <li> Resource changes are disallowed during resource change event 
 *   notification.</li>
 * </ul>
 * @see IResourceMapping
 * @see IResource#PROJECT_ROOT
 * @see #newMapping
 * @see #getMappings
 * @see #addMapping
 */
public void removeMapping(String name) throws CoreException;
/**
 * Sets the list of build command to run when building the described project.
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param buildSpec the array of build commands to run
 * @see IProject#setDescription
 * @see #getBuildSpec
 * @see #newCommand
 */
public void setBuildSpec(ICommand[] buildSpec);
/**
 * Sets the comment for the described project
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param comment the comment for the described project
 * @see IProject#setDescription
 * @see #getComment
 */
public void setComment(String comment);
/**
 * Sets the local file system location for the described project.
 * If <code>null</code> is specified, the default location is used.
 * <p>
 * Setting the location on a description for a project which already
 * exists has no effect; the new project location is ignored when the
 * description is set on the already existing project. This method is 
 * intended for use on descriptions for new projects or for destination 
 * projects for <code>copy</code> and <code>move</code>.
 * </p>
 * <p>
 * This operation maps the root folder of the project to the exact location
 * provided.  For example, if the location for project named "P" is set
 * to the path c:\my_plugins\Project1, the file resource at workspace path
 * /P/index.html  would be stored in the local file system at 
 * c:\my_plugins\Project1\index.html.
 * </p>
 *
 * @param location the location for the described project or <code>null</code>
 * @see #getLocation
 */
public void setLocation(IPath location);
/**
 * Sets the name of the described project
 * <p>
 * Setting the name on a description and then setting the 
 * description on the project has no effect; the new name is ignored.
 * </p>
 * <p>
 * Creating a new project with a description name which doesn't
 * match the project handle name results in the description name
 * being ignored, the project will be creating using the name
 * in the handle.
 * </p>
 *
 * @param projectName the name of the described project
 * @see IProject#setDescription
 * @see #getName
 */
public void setName(String projectName);
/** 
 * Sets the list of natures associated with the described project.
 * A project created with this description will have these natures
 * added to it in the given order.
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param natures the list of natures
 * @see IProject#setDescription
 * @see #getNatureIds
 */ 
public void setNatureIds(String[] natures);
/**
 * Sets the referenced projects, ignoring any duplicates.
 * The order of projects is preserved.
 * The projects need not exist in the workspace.
 * <p>
 * Users must call <code>IProject.setDescription</code> before changes 
 * made to this description take effect.
 * </p>
 *
 * @param projects a list of projects
 * @see IProject#setDescription
 * @see #getReferencedProjects
 */
public void setReferencedProjects(IProject[] projects);
}
