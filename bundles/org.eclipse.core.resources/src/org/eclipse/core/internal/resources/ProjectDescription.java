/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.utils.Assert;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;


public class ProjectDescription extends ModelObject implements IProjectDescription {
	// fields
	protected IPath location;
	protected IProject[] projects;
	protected String[] natures;
	protected ICommand[] buildSpec;
	protected Map mappings;
	protected String comment =""; //$NON-NLS-1$

	// constants
	private static IProject[] EMPTY_PROJECT_ARRAY = new IProject[0];
	private static String[] EMPTY_STRING_ARRAY = new String[0];
	private static ICommand[] EMPTY_COMMAND_ARRAY = new ICommand[0];
public ProjectDescription() {
	super();
	buildSpec = EMPTY_COMMAND_ARRAY;
	projects = EMPTY_PROJECT_ARRAY;
	natures = EMPTY_STRING_ARRAY;
	mappings = new HashMap(3);
}
public void addMapping(IResourceMapping mapping) {
	Assert.isLegal(mapping != null);
	mappings.put(mapping.getName(), mapping);
}
/**
 * Returns a copy of this project description that only contains shared
 * project description state.
 */
public ProjectDescription copyWithSharedState() {
	ProjectDescription result = new ProjectDescription();
	result.setBuildSpec(buildSpec);
	result.setComment(comment);
	result.setName(name);
	result.setNatureIds(natures);
	result.setReferencedProjects(projects);
	return result;
}
/**
 * Returns a copy of this project description that only contains non-shared
 * project description state.
 */
public ProjectDescription copyWithPrivateState() {
	ProjectDescription result = new ProjectDescription();
	result.setLocation(location);
	result.setMappings(mappings);
	return result;
}

/**
 * @see IProjectDescription
 */
public ICommand[] getBuildSpec() {
	return getBuildSpec(true);
}
public ICommand[] getBuildSpec(boolean makeCopy) {
	if (buildSpec == null)
		return EMPTY_COMMAND_ARRAY;
	return makeCopy ? (ICommand[]) buildSpec.clone() : buildSpec;
}
/**
 * @see IProjectDescription
 */
public String getComment() {
	return comment;
}
/**
 * @see IProjectDescription#getLocation
 */
public IPath getLocation() {
	return location;
}
public IResourceMapping getMapping(String name) {
	return (IResourceMapping) mappings.get(name);
}
/**
 * Returns a copy of the project mappings, or null if there are none.
 */
public Map getMappings() {
	return getMappings(true);
}
/**
 * The makeCopy parameter is used for optimization. As this method is not API, in
 * cases we only want to take a look at the mappings we don't need to make a copy
 * of it.  Returns null if no mappings are defined.
 */
public Map getMappings(boolean makeCopy) {
	if (!makeCopy)
		return mappings;
	return new HashMap(mappings);
}
/**
 * @see IProjectDescription
 */
public String[] getNatureIds() {
	return getNatureIds(true);
}
public String[] getNatureIds(boolean makeCopy) {
	if (natures == null)
		return EMPTY_STRING_ARRAY;
	return makeCopy ? (String[]) natures.clone() : natures;
}
/**
 * @see IProjectDescription
 */
public IProject[] getReferencedProjects() {
	return getReferencedProjects(true);
}
public IProject[] getReferencedProjects(boolean makeCopy) {
	if (projects == null)
		return EMPTY_PROJECT_ARRAY;
	return makeCopy ? (IProject[]) projects.clone() : projects;
}
/**
 * @see IProjectDescription#hasNature
 */
public boolean hasNature(String natureID) {
	String[] natureIDs = getNatureIds(false);
	for (int i = 0; i < natureIDs.length; ++i) {
		if (natureIDs[i].equals(natureID)) {
			return true;
		}
	}
	return false;
}
/**
 * @see IProjectDescription
 */
public ICommand newCommand() {
	return new BuildCommand();
}
/**
 * @see IProjectDescription#newMapping
 */
public IResourceMapping newMapping(String name, IPath local) {
	return new ResourceMapping(name, local);
}
public void removeMapping(String name) {
	mappings.remove(name);
}
/**
 * @see IProjectDescription
 */
public void setBuildSpec(ICommand[] value) {
	Assert.isLegal(value != null);
	buildSpec = (ICommand[]) value.clone();
}
/**
 * @see IProjectDescription
 */
public void setComment(String value) {
	comment = value;
}
/**
 * @see IProjectDescription#setLocation
 */
public void setLocation(IPath location) {
	this.location = location;
}
public void setMappings(Map mappings) {
	Assert.isLegal(mappings != null);
	this.mappings = mappings;
}
/**
 * @see IProjectDescription
 */
public void setName(String value) {
	super.setName(value);
}
/**
 * @see IProjectDescription
 */
public void setNatureIds(String[] value) {
	Assert.isLegal(value != null);
	natures = (String[]) value.clone();
}
/**
 * @see IProjectDescription
 */
public void setReferencedProjects(IProject[] value) {
	Assert.isLegal(value != null);
	IProject[] result = new IProject[value.length];
	int count = 0;
	for (int i = 0; i < value.length; i++) {
		IProject project = value[i];
		boolean found = false;
		// scan to see if there are any other projects by the same name
		for (int j = 0; j < value.length; j++) {
			if (i != j && project.equals(value[j]))
				found = true;
		}
		if (!found)
			result[count++] = project;
	}
	projects = new IProject[count];
	System.arraycopy(result, 0, projects, 0, count);
}
}
