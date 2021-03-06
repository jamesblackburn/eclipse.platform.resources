/**********************************************************************
 * Copyright (c) 2003, 2004 Geoff Longman and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Geoff Longman - Initial API and implementation
 * IBM - Tightening integration with existing Platform
 * Geoff Longman - added ability to track extension registry changes
 **********************************************************************/
package org.eclipse.core.tools.resources.markers;

import java.util.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.dynamichelpers.*;
import org.eclipse.ui.PlatformUI;

/**
 * Builds a model of all the markers defined in the Workbench
 */
public class MarkerExtensionModel {

	private static final boolean DEBUG = false; // uncomment to get model dumps

	static public class MarkerInfo {

		public MarkerInfo(String id, boolean persistent, List supers, List attributes) {
			this.id = id;
			this.persistent = persistent;
			this.declaredSupers = supers;
			this.declaredAttributes = attributes;
		}

		public boolean persistent;
		public List declaredSupers = empty;
		public List declaredAttributes = empty;
		public String id;
	}

	public static interface MarkerModelChangedListener {
		public void markerModelChanged(MarkerExtensionModel newModel);
	}

	static List empty = Collections.unmodifiableList(new ArrayList());
	static public String RESOURCES_PROBLEM = IMarker.PROBLEM;
	static public String RESOURCES_TASK = IMarker.TASK;
	static public String RESOURCES_BOOKMARK = IMarker.BOOKMARK;
	static public String RESOURCES_TEXT = IMarker.TEXT;
	Map markerMap = new HashMap();

	/**
	 * Constructor for MarkerExtensionHandler.
	 */
	public MarkerExtensionModel() {
		super();
		readMarkerDeclarations();
		registerForExtensionChanges();
	}

	public synchronized MarkerInfo getInfo(String id) {
		return (MarkerInfo) markerMap.get(id);
	}

	private void registerForExtensionChanges() {
		IExtensionChangeHandler changeHandler = new IExtensionChangeHandler() {
			public void addExtension(IExtensionTracker tracker, IExtension extension) {
				readMarkerDeclarations();
			}

			public void removeExtension(IExtension extension, Object[] objects) {
				readMarkerDeclarations();
			}
		};
		IExtensionTracker tracker = PlatformUI.getWorkbench().getExtensionTracker();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);
		tracker.registerHandler(changeHandler, ExtensionTracker.createExtensionPointFilter(point));
	}

	/*
	 * Retrieve the marker defn info out of the extension point.
	 */
	synchronized void readMarkerDeclarations() {
		markerMap.clear();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);
		if (point != null) {
			// Gather all registered marker types.
			IExtension[] extensions = point.getExtensions();
			for (int i = 0; i < extensions.length; ++i) {
				IExtension extension = extensions[i];
				String identifier = extension.getUniqueIdentifier();
				boolean persistent = false;
				ArrayList supersList = new ArrayList();
				ArrayList attributes = new ArrayList();
				IConfigurationElement[] configElements = extension.getConfigurationElements();
				for (int j = 0; j < configElements.length; ++j) {
					IConfigurationElement elt = configElements[j];
					if (elt.getName().equalsIgnoreCase("super")) {
						String sup = elt.getAttribute("type");
						if (sup != null)
							supersList.add(sup);
					} else if (elt.getName().equalsIgnoreCase("attribute")) {
						String attr = elt.getAttribute("name");
						if (attr != null)
							attributes.add(attr);
					} else if (elt.getName().equalsIgnoreCase("persistent")) {
						String value = elt.getAttribute("value");
						persistent = "yes".equalsIgnoreCase(value) ? true : false;
					}
				}
				MarkerInfo info = new MarkerInfo(identifier, persistent, supersList, attributes);
				markerMap.put(identifier, info);
			}
		}
		if (DEBUG)
			dumpMarkerTypes();
	}

	// a cruddy debugging tool. Dumps the model out in pseudo xml
	// so it's easier to see the relationships!
	private void dumpMarkerTypes() {
		for (Iterator iter = markerMap.keySet().iterator(); iter.hasNext();) {
			String type = (String) iter.next();
			dumpMarkerType(type, 0);
		}
	}

	private void dumpMarkerType(String type, int indent) {
		MarkerInfo mtype = (MarkerInfo) markerMap.get(type);
		printIndented(indent, "<marker type='" + type + "' ");
		if (mtype == null) {
			System.out.println("not-found='true'/>");
		} else {
			boolean hasAttrs = !mtype.declaredAttributes.isEmpty();
			boolean hasSupers = !mtype.declaredSupers.isEmpty();
			if (!hasAttrs && !hasSupers) {
				System.out.println("/>");
				return;
			}
			if (hasAttrs) {
				System.out.println();
				printlnIndented(indent + 1, " attrs='" + mtype.declaredAttributes + "'" + (hasSupers ? ">" : "/>"));
				if (!hasSupers)
					return;
			} else {
				System.out.println(">");
			}
			printlnIndented(indent + 1, "<supers>");
			if (hasSupers) {
				for (Iterator iter = mtype.declaredSupers.iterator(); iter.hasNext();) {
					String superType = (String) iter.next();
					dumpMarkerType(superType, indent + 2);
				}
			}
			printlnIndented(indent + 1, "</supers>");
			printlnIndented(indent, "</marker>");
		}
	}

	private void printIndented(int indent, Object value) {
		for (int i = 0; i < indent; i++)
			System.out.print("  ");
		System.out.print(value);
	}

	private void printlnIndented(int indent, Object value) {
		printIndented(indent, value);
		System.out.println();
	}
}