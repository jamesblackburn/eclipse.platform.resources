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
package org.eclipse.core.internal.resources;

import java.util.*;

import org.eclipse.core.internal.localstore.CoreFileSystemLibrary;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Class that checks for overlapping file system locations between various projects and resource
 * mappings.
 */
public class LocationValidator {
protected static IStatus createStatus(IPath one, IPath two) {
	String message = Policy.bind("resources.overlapLocal", one.toString(), two.toString()); //$NON-NLS-1$
	return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
}
/**
 * Returns true if the two given paths overlap, and false otherwise.  Null
 * paths never overlap.
 */
protected static boolean isOverlapping(IPath one, IPath two) {
	if (one == null || two == null)
		return false;
	// If we are on a case-insensitive file system then we will convert to all lowercase.
	if (!CoreFileSystemLibrary.isCaseSensitive()) {
		one = new Path(one.toOSString().toLowerCase());
		two = new Path(two.toOSString().toLowerCase());
	}
	return one.isPrefixOf(two) || two.isPrefixOf(one);
}
public static IStatus validateLocation(IPath location, IProject projectContext, String mappingContext, boolean ignoreSameProject) {
	//a null location is always valid
	if (location == null)
		return ResourceStatus.OK_STATUS;
	//if the location doesn't have a device, see if the OS will assign one
	if (location.getDevice() == null)
		location = new Path(location.toFile().getAbsolutePath());
	// test if the given location overlaps the default default location
	IPath defaultDefaultLocation = Platform.getLocation();
	if (isOverlapping(location, defaultDefaultLocation)) {
		return createStatus(location, defaultDefaultLocation);
	}
	//iterate over all existing projects in the workspace
	IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	for (int i = 0; i < allProjects.length; i++) {
		Project project = (Project)allProjects[i];
		boolean sameProject = project.equals(projectContext);
		if (ignoreSameProject && sameProject)
			continue;

		IProjectDescription description = project.internalGetDescription();
		if (description == null)
			//can only happen if this project is in the middle of being created.
			continue;
		//don't compare project location if it's the one being validated
		if (!sameProject || mappingContext != null) {
			//compare the location with the default location for this project
			IPath projectLocation = description.getLocation();
			if (isOverlapping(projectLocation, location))
				return createStatus(location, projectLocation);
		}
		//compare the location with all the resource mappings for this project
		Iterator mappings = description.getMappings().keySet().iterator();
		while (mappings.hasNext()) {
			IResourceMapping mapping = (IResourceMapping)mappings.next();
			//don't compare mapping if it's the one being validated
			if (!sameProject || !mapping.getName().equals(mappingContext)) {
				IPath mappingLocation = mapping.getLocation();
				if (isOverlapping(mappingLocation, location))
					return createStatus(mappingLocation, location);
			}
		}
	}
	return ResourceStatus.OK_STATUS;
}
/**
 * Validates the the given project description contains valid location and mapping settings.
 * This ensures that the location and mappings don't overlap each other, or with the location and
 * mappings of any other project.  The project's current location and mappings (if it exists),
 * are ignored.  Returns an OK status if the project description is valid, otherwise a WARNING
 * status describing which paths overlap.
 */
public static IStatus validateProjectDescription(IProject project, IProjectDescription description) {
	IPath location = description.getLocation();
	Map mappingMap = description.getMappings();
	//first check for overlap within the project description
	IStatus result = validateWithinDescription(location, mappingMap);
	if (!result.isOK())
		return result;
	//now validate against other projects
	result = validateLocation(location, project, null, true);
	if (!result.isOK())
		return result;
	for (Iterator it = mappingMap.values().iterator(); it.hasNext();) {
		location = ((IResourceMapping)it.next()).getLocation();
		result = validateLocation(location, project, null, true);
		if (!result.isOK())
			return result;
	}
	return ResourceStatus.OK_STATUS;
}

/**
 * Validates a location and set of mappings for internal consistency.  Returns a
 * WARNING status if any of the mappings overlap with the location or 
 * with each other, and an OK status otherwise.
 */
private static IStatus validateWithinDescription(IPath location, Map mappingMap) {
	//if there are no mappings then there can't be any overlap
	if (mappingMap.size() == 0)
		return ResourceStatus.OK_STATUS;
	//list of IResourceMappings that need checking
	ArrayList toCheck = new ArrayList(mappingMap.values());
	//list of IPath locations that have been checked
	ArrayList checked = new ArrayList(toCheck.size() + 1);
	if (location != null)
		checked.add(location);
	//iterate over toCheck, and check for overlap with all locations that have already been checked.
	//this is O(n^2) but the number of mappings should be fairly small
	for (int i = 0, imax = toCheck.size(); i < imax; i++) {
		IPath locationToCheck = ((IResourceMapping)toCheck.get(i)).getLocation();
		if (locationToCheck != null) {
			for (int j = 0, jmax = checked.size(); j < jmax; j++) {
				IPath checkedLocation = (IPath)checked.get(j);
				if (isOverlapping(locationToCheck, checkedLocation))
					return createStatus(locationToCheck, checkedLocation);
			}
			checked.add(locationToCheck);
		}
	}
	return ResourceStatus.OK_STATUS;
}
}