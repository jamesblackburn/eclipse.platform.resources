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
package org.eclipse.core.internal.events;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.resources.ModelObject;
import org.eclipse.core.resources.ICommand;
public class BuildCommand extends ModelObject implements ICommand {
	protected HashMap arguments;
	/** Cached hash code for performance */
	protected int hash = -1;
	
public BuildCommand() {
	super(""); //$NON-NLS-1$
	this.arguments = new HashMap(0);
}
public BuildCommand(String builderName, Map args) {
	super(builderName);
	this.arguments = args == null ? new HashMap(0) : new HashMap(args);
}
public Object clone() {
	BuildCommand result = null;
	result = (BuildCommand) super.clone();
	if (result == null)
		return null;
	result.setArguments(getArguments());
	return result;
}
public boolean equals(Object object) {
	if (this == object)
		return true;
	if (!(object instanceof BuildCommand))
		return false;
	//for performance compare the cached hash value first
	if (hashCode() != object.hashCode())
		return false;
	BuildCommand command = (BuildCommand) object;
	// equal if same builder name and equal argument tables
	return (name == null ? command.name == null : name.equals(command.name)) &&
		(arguments == null ? command.arguments == null : arguments.equals(command.arguments));
}
/**
 * @see ICommand#getArguments
 */
public Map getArguments() {
	return getArguments(true);
}
public Map getArguments(boolean makeCopy) {
	return arguments == null ? null : (makeCopy ? (Map) arguments.clone() : arguments);
}
/**
 * @see ICommand#getBuilderName
 */
public String getBuilderName() {
	return getName();
}
public int hashCode() {
	//lazily compute and cache hashcode
	if (hash == -1) {
		String name = getName();
		hash = name == null ? 17 : name.hashCode() * 37;
		if (arguments != null)
			hash += arguments.hashCode();
		//make sure it is never the same as the default value
		if (hash == -1)
			hash++;
	}
	return hash;
}
/**
 * @see ICommand#setArguments
 */
public void setArguments(Map value) {
	// copy parameter for safety's sake
	arguments = value == null ? null : new HashMap(value);
	hash = -1;
}
/**
 * @see ICommand#setBuilderName
 */
public void setBuilderName(String value) {
	//don't allow builder name to be null
	setName(value == null ? "" : value); //$NON-NLS-1$
	hash = -1;
}
}
