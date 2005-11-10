/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources.mapping;

import java.util.*;
import org.eclipse.core.internal.resources.mapping.ModelProviderManager;
import org.eclipse.core.internal.resources.mapping.SimpleResourceMapping;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;

/**
 * Represents the provider of a logical model. The main purpose of this
 * API is to support batch operations on sets of <code>ResourceMapping</code>
 * objects that are part of the same model.
 * 
 * TODO: include xml snippet
 * 
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @since 3.2
 */
public abstract class ModelProvider extends PlatformObject {
	
	/**
	 * The model provider id of the Resources model.
	 */
	public static final String RESOURCE_MODEL_PROVIDER_ID = "org.eclipse.core.resources.modelProvider"; //$NON-NLS-1$

	private IModelProviderDescriptor descriptor;

	/**
	 * Return the descriptor for the model provider of the given id
	 * or <code>null</code> if the provider has not been registered.
	 * @param id a model provider id.
	 * @return the descriptor for the model provider of the given id
	 * or <code>null</code> if the provider has not been registered
	 */
	public static IModelProviderDescriptor getModelProviderDescriptor(String id) {
		IModelProviderDescriptor[] descs = ModelProviderManager.getDefault().getDescriptors();
		for (int i = 0; i < descs.length; i++) {
			IModelProviderDescriptor descriptor = descs[i];
			if (descriptor.getId().equals(id)) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * Return the descriptors for all model providers that are
	 * registered.
	 * @return the descriptors for all model providers that are
	 * registered.
	 */
	public static IModelProviderDescriptor[] getModelProviderDescriptors() {
		return ModelProviderManager.getDefault().getDescriptors();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ModelProvider) {
			ModelProvider other = (ModelProvider) obj;
			return other.getDescriptor().getId().equals(getDescriptor().getId());
		}
		return super.equals(obj);
	}

	/**
	 * Return the descriptor of this model provider. The descriptor
	 * is set during initialization so implements cannot call this method
	 * until after the <code>initialize</code> method is invoked.
	 * @return the descriptor of this model provider
	 */
	public final IModelProviderDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Return the resource mappings that cover the given resource.
	 * By default a single resource mapping that traverses the given resource
	 * deeply is returned. Subclass may override.
	 * 
	 * @param resource the resource
	 * @param context a resource mapping context
	 * @param monitor a progress monitor
	 * @return the resource mappings that cover the given resource.
	 * @throws CoreException 
	 */
	public ResourceMapping[] getMappings(IResource resource, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		return new ResourceMapping[] {new SimpleResourceMapping(resource)};
	}

	/**
	 * Return the set of mappings that cover the given resources.
	 * This method is used to map operations on resources to
	 * operations on resource mappings. By default, this method
	 * calls <code>getMapping(IResource)</code> for each resource.
	 * Subclasses may override.
	 * @param resources
	 * @param monitor 
	 * @param context 
	 * @return the set of mappings that cover the given resources
	 * @throws CoreException 
	 */
	public ResourceMapping[] getMappings(IResource[] resources, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		Set mappings = new HashSet();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			mappings.addAll(Arrays.asList(getMappings(resource, context, monitor)));
		}
		return (ResourceMapping[]) mappings.toArray(new ResourceMapping[mappings.size()]);
	}

	/**
	 * Return a set of traversals that cover the given resource mappings. The
	 * provided mappings must be from this provider or one of the providers this
	 * provider extends.
	 * <p>
	 * The default implementation accumulates the traversals from the given
	 * mappings. Subclasses can override to provide a more optimal
	 * transformation.
	 * 
	 * @param mappings the mappings being mapped to resources
	 * @param context the context used to determine the set of traversals that
	 *            cover the mappings
	 * @param monitor a progress monitor
	 * @return a set of traversals that cover the given mappings
	 * @throws CoreException
	 */
	public ResourceTraversal[] getTraversals(ResourceMapping[] mappings, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100 * mappings.length);
			List traversals = new ArrayList();
			for (int i = 0; i < mappings.length; i++) {
				ResourceMapping mapping = mappings[i];
				traversals.addAll(Arrays.asList(mapping.getTraversals(context, new SubProgressMonitor(monitor, 100))));
			}
			return (ResourceTraversal[]) traversals.toArray(new ResourceTraversal[traversals.size()]);
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getDescriptor().getId().hashCode();
	}

	/**
	 * This method is called by the model provider framework when the model
	 * provider is instantiated. This method should not be called by clients and
	 * cannot be overridden by subclasses. However, it invokes the
	 * <code>initialize</code> method once the descriptor is set so subclasses
	 * can override that method if they need to do additional initialization.
	 * 
	 * @param desc
	 *            the description of the provider as it appears in the plugin
	 *            manifest
	 */
	public final void init(IModelProviderDescriptor desc) {
		if (descriptor != null)
			// prevent subsequent calls from damaging this instance
			return;
		descriptor = desc;
		initialize();
	}

	/**
	 * Initialization method that is called after the descriptor
	 * of this provider is set. Subclasses may override.
	 */
	protected void initialize() {
		// Do nothing	
	}
}