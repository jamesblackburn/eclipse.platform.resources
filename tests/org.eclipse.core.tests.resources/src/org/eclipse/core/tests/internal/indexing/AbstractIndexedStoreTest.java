/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tests.internal.indexing;

import junit.framework.TestCase;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * 
 */
public abstract class AbstractIndexedStoreTest extends TestCase {
	public AbstractIndexedStoreTest() {
		super();
	}
	public AbstractIndexedStoreTest(String name) {
		super(name);
	}
	protected String getFileName() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().append("test.dat").toOSString();
	}
	protected void println(String string) {
		//don't log anything during tests
	}
	protected void printHeading(String string) {
		//don't log anything during tests
	}
	
}
