/*******************************************************************************
 * Copyright (c) 2008, 2009 Freescale Semiconductor and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.resources.IFileInfoFilterFactory;

import org.eclipse.core.resources.IFilterDescriptor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;
import org.eclipse.core.runtime.*;

/**
 *  This class collects all the registered {@link IFileInfoFilterFactory} instances along 
 *  with their properties.
 * @since 3.6
 */
class FilterTypeManager implements IManager {

	private static final String FILTER_PROVIDER = "filterProvider";  //$NON-NLS-1$

	private HashMap/*<String, FilterDescriptor>*/  factories = new HashMap();

	public FilterTypeManager() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES,ResourcesPlugin.PT_FILTER_PROVIDERS);
		if (point != null) {
			IExtension[] ext = point.getExtensions();
			// initial population
			for (int i = 0; i < ext.length; i++) {
				IExtension extension = ext[i];
				processExtension(extension);
			}
			Platform.getExtensionRegistry().addListener(new IRegistryEventListener() {
				public void added(IExtension[] extensions) {
					for (int i = 0; i < extensions.length; i++)
						processExtension(extensions[i]);
				}
				public void added(IExtensionPoint[] extensionPoints) {
					// nothing to do
				}
				public void removed(IExtension[] extensions) {
					for (int i = 0; i < extensions.length; i++)
						processRemovedExtension(extensions[i]);
				}
				public void removed(IExtensionPoint[] extensionPoints) {
					// nothing to do
				}
			});
		}
	}
	
	public IFilterDescriptor getFilterDescriptor(String id) {
		return (IFilterDescriptor) factories.get(id);
	}

	public IFilterDescriptor[] getFilterDescriptors() {
		return (IFilterDescriptor[]) factories.values().toArray(new IFilterDescriptor[0]);
	}

	protected void processExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase(FILTER_PROVIDER)) {
				try {
					IFilterDescriptor desc = new FilterDescriptor(element);
					factories.put(desc.getId(), desc);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void processRemovedExtension(IExtension extension) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase(FILTER_PROVIDER)) {
				try {
					IFilterDescriptor desc = new FilterDescriptor(element, false);
					factories.remove(desc.getId());
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void shutdown(IProgressMonitor monitor) {
		//nothing to do
	}

	public void startup(IProgressMonitor monitor) {
		//nothing to do
	}
}