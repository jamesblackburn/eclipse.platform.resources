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

import org.eclipse.core.internal.utils.Assert;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;


/**
 *
 */
public class ResourceMapping extends ModelObject implements IResourceMapping {
    protected IPath location;
public ResourceMapping(String name, IPath local) {
    super();
    this.setName(name);
    location = local;
}
/**
 * @see IResourceMapping#getLocation
 */
public IPath getLocation() {
    return location;
}
/**
 * @see IResourceMapping#setLocation
 */
public void setLocation(IPath value) {
    location = value;
}
/**
 * @see IResourceMapping#setName
 */
public void setName(String name) {
    Assert.isTrue(ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE | IResource.FOLDER).isOK(), Policy.bind("invalidMapName"));
    this.name = name;
}
public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    sb.append(name);
    sb.append(", ");
    sb.append(location);
    sb.append("}");
    return sb.toString();
}
}
