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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.internal.events.BuilderPersistentInfo;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Reads .tree files in format version 3.
 */
public class WorkspaceTreeReader_3 extends WorkspaceTreeReader_2 {
public WorkspaceTreeReader_3(Workspace workspace) {
	super(workspace);
}
protected int getVersion() {
	return ICoreConstants.WORKSPACE_TREE_VERSION_3;
}
protected void readBuildersPersistentInfo(DataInputStream input, List builders, IProgressMonitor monitor) throws IOException {
	monitor = Policy.monitorFor(monitor);
	try {
		int builderCount = input.readInt();
		for (int i = 0; i < builderCount; i++) {
			BuilderPersistentInfo info = new BuilderPersistentInfo();
			info.setProjectName(input.readUTF());
			info.setBuilderName(input.readUTF());
			// read interesting projects
			int n = input.readInt();
			IProject[] projects = new IProject[n];
			for (int j = 0; j < n; j++)
				projects[j] = workspace.getRoot().getProject(input.readUTF());
			info.setInterestingProjects(projects);
			//read build spec position
			info.setBuildSpecPosition(input.readInt());
			builders.add(info);
		}
	} finally {
		monitor.done();
	}
}
}