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
package org.eclipse.core.internal.events;

import org.eclipse.core.resources.IResource;

/**
 * Class used for broadcasting internal workspace lifecycle events.  There is a
 * singleton instance, so no listener is allowed to keep references to the event
 * after the notification is finished.
 */
public class LifecycleEvent {
	//constants for kinds of internal workspace lifecycle events
	public static final int PRE_PROJECT_CLOSE = 0x01;
	public static final int PRE_PROJECT_DELETE = 0x02;
	public static final int PRE_PROJECT_OPEN = 0x04;
	public static final int PRE_PROJECT_MOVE = 0x08;
	public static final int POST_LINK_CREATE = 0x10;
	public static final int PRE_LINK_DELETE = 0x20;
	public static final int PRE_LINK_MOVE = 0x40;

	/**
	 * The kind of event
	 */
	public int kind;
	/**
	 * For events that only involve one resource, this is it.  More
	 * specifically, this is used for all non-move events. For move events, this
	 * resource represents the source of the move.
	 */
	public IResource resource;
	/**
	 * For move events, this resource represents the destination of the move.
	 */
	public IResource newResource;
	
	/**
	 * The update flags for the event.
	 */
	public int updateFlags;
	
	private static final LifecycleEvent instance = new LifecycleEvent();
	private LifecycleEvent() {
		super();
	}
	public LifecycleEvent newEvent(int kind, IResource resource) {
		this.kind = kind;
		this.resource = resource;
		this.newResource = null;
		this.updateFlags = 0;
		return this;
	}
	public LifecycleEvent newEvent(int kind, IResource oldResource, IResource newResource, int updateFlags) {
		this.kind = kind;
		this.resource = oldResource;
		this.newResource = null;
		this.updateFlags = 0;
		return this;
	}
}