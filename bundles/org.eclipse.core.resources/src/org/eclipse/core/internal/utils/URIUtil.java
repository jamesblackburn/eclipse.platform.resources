/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.utils;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.IPath;

/**
 * Static utility methods for manipulating URIs.
 */
public class URIUtil {
	/**
	 * Not intended for instantiation.
	 */
	private URIUtil() {
		super();
	}
	/**
	 * Converts a path to a URI
	 */
	public static URI toURI(IPath path) {
		if (path.isAbsolute())
			return path.toFile().toURI();
		try {
			return new URI(path.toString());
		} catch (URISyntaxException e) {
			//is it possible that a path can't be represented as a URI?
			throw new Error(e);
		}
	}
}
