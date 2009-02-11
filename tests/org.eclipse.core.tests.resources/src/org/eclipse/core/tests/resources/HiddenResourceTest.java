/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class HiddenResourceTest extends ResourceTest {
	public HiddenResourceTest() {
		super();
	}

	public HiddenResourceTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(HiddenResourceTest.class);
	}

	public void testRefreshLocal() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		listener.addExpectedChange(subFile, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		getWorkspace().addResourceChangeListener(listener);
		try {
			setHidden("3.0", folder, true, IResource.DEPTH_ZERO);
			ensureOutOfSync(subFile);
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
				assertTrue(listener.getMessage(), listener.isDeltaValid());
			} catch (CoreException e) {
				fail("3.1", e);
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as hidden resources should always be found.
	 */
	public void testFindMember() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// no hidden resources
		assertEquals("1.0", project, root.findMember(project.getFullPath()));
		assertEquals("1.1", folder, root.findMember(folder.getFullPath()));
		assertEquals("1.2", file, root.findMember(file.getFullPath()));
		assertEquals("1.3", subFile, root.findMember(subFile.getFullPath()));

		// the folder is hidden
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		assertEquals("2.1", project, root.findMember(project.getFullPath()));
		assertEquals("2.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("2.3", file, root.findMember(file.getFullPath()));
		assertEquals("2.4", subFile, root.findMember(subFile.getFullPath()));

		// all are hidden
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		assertEquals("3.1", project, root.findMember(project.getFullPath()));
		assertEquals("3.2", folder, root.findMember(folder.getFullPath()));
		assertEquals("3.3", file, root.findMember(file.getFullPath()));
		assertEquals("3.4", subFile, root.findMember(subFile.getFullPath()));
	}

	/**
	 * Resources which are marked as hidden are not included in #members
	 * calls unless specifically included by calling #members(IContainer.INCLUDE_HIDDEN)
	 */
	public void testMembers() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		IResource[] members = null;
		ensureExistsInWorkspace(resources, true);

		// Initial values should be false.
		assertHidden("1.0", project, false, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		try {
			members = project.members();
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// +1 for the project description file
		assertEquals("2.1", 3, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("2.2", e);
		}
		assertEquals("2.3", 1, members.length);

		// Set the values.
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		assertHidden("3.1", project, true, IResource.DEPTH_INFINITE);

		// Check the values
		assertHidden("4.0", project, true, IResource.DEPTH_INFINITE);

		// Check the calls to #members
		try {
			members = project.members();
		} catch (CoreException e) {
			fail("5.0", e);
		}
		assertEquals("5.1", 0, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("5.2", e);
		}
		assertEquals("5.3", 0, members.length);

		// FIXME: add the tests for #members(int)

		// reset to false
		setHidden("6.0", project, false, IResource.DEPTH_INFINITE);
		assertHidden("6.1", project, false, IResource.DEPTH_INFINITE);

		// Check the calls to members(IResource.NONE);
		try {
			members = project.members(IResource.NONE);
		} catch (CoreException e) {
			fail("7.0", e);
		}
		// +1 for the project description file
		assertEquals("7.1", 3, members.length);
		try {
			members = project.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("7.2", e);
		}
		// +1 for the project description file
		assertEquals("7.3", 3, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("7.4", e);
		}
		assertEquals("7.5", 1, members.length);

		// Set one of the children to be HIDDEN and try again
		setHidden("8.0", folder, true, IResource.DEPTH_ZERO);
		try {
			members = project.members();
		} catch (CoreException e) {
			fail("8.1", e);
		}
		// +1 for project description, -1 for hidden folder
		assertEquals("8.2", 2, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("8.3", e);
		}
		assertEquals("8.4", 1, members.length);
		try {
			members = project.members(IResource.NONE);
		} catch (CoreException e) {
			fail("8.5", e);
		}
		// +1 for project description, -1 for hidden folder
		assertEquals("8.6", 2, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("8.7", e);
		}
		assertEquals("8.8", 1, members.length);
		try {
			members = project.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("8.9", e);
		}
		// +1 for project description
		assertEquals("8.10", 3, members.length);
		try {
			members = folder.members();
		} catch (CoreException e) {
			fail("8.11", e);
		}
		assertEquals("8.12", 1, members.length);

		// Set all the resources to be hidden
		setHidden("9.0", project, true, IResource.DEPTH_INFINITE);
		assertHidden("9.1", project, true, IResource.DEPTH_INFINITE);
		try {
			members = project.members(IResource.NONE);
		} catch (CoreException e) {
			fail("9.2", e);
		}
		assertEquals("9.3", 0, members.length);
		try {
			members = folder.members(IResource.NONE);
		} catch (CoreException e) {
			fail("9.4", e);
		}
		assertEquals("9.5", 0, members.length);
		try {
			members = project.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("9.6", e);
		}
		// +1 for project description
		assertEquals("9.7", 3, members.length);
		try {
			members = folder.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("9.8", e);
		}
		assertEquals("9.9", 1, members.length);
	}

	/**
	 * Resources which are marked as hidden resources should not be visited by
	 * resource visitors.
	 */
	public void testAccept() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);
		IResource description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);

		// default case, no hidden resources
		ResourceVisitorVerifier visitor = new ResourceVisitorVerifier();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		try {
			project.accept(visitor);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		try {
			project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		} catch (CoreException e) {
			fail("1.2", e);
		}
		assertTrue("1.3." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		try {
			project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("1.4", e);
		}
		assertTrue("1.5." + visitor.getMessage(), visitor.isValid());

		// set the folder to be hidden. It and its children should
		// be ignored by the visitor
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		try {
			project.accept(visitor);
		} catch (CoreException e) {
			fail("2.1", e);
		}
		assertTrue("2.2." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(project);
		visitor.addExpected(file);
		visitor.addExpected(description);
		try {
			project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		} catch (CoreException e) {
			fail("2.3", e);
		}
		assertTrue("2.4." + visitor.getMessage(), visitor.isValid());
		// should see all resources if we include the flag
		visitor.reset();
		visitor.addExpected(resources);
		visitor.addExpected(description);
		try {
			project.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("2.5", e);
		}
		assertTrue("2.6." + visitor.getMessage(), visitor.isValid());
		// should NOT visit the folder and its members if we call accept on it directly
		visitor.reset();
		try {
			folder.accept(visitor);
		} catch (CoreException e) {
			fail("2.7", e);
		}
		assertTrue("2.8." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		try {
			folder.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		} catch (CoreException e) {
			fail("2.9", e);
		}
		assertTrue("2.10." + visitor.getMessage(), visitor.isValid());

		visitor.reset();
		visitor.addExpected(folder);
		visitor.addExpected(subFile);
		try {
			folder.accept(visitor, IResource.DEPTH_INFINITE, IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail("2.11", e);
		}
		assertTrue("2.11." + visitor.getMessage(), visitor.isValid());
	}

	public void testCopy() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = new IResource[] {destProject, destFolder, destFile, destSubFile};
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be hidden
		setHidden("1.0", folder, true, IResource.DEPTH_ZERO);
		// copy the project
		int flags = IResource.FORCE;
		try {
			project.copy(destProject.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		assertExistsInWorkspace("1.2", resources);
		assertExistsInWorkspace("1.3", destResources);

		// do it again and but just copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		try {
			folder.copy(destFolder.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}
		assertExistsInWorkspace("2.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.3", new IResource[] {destFolder, destSubFile});

		// set all the resources to be hidden
		// copy the project
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		try {
			project.copy(destProject.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}
		assertExistsInWorkspace("3.2", resources);
		assertExistsInWorkspace("3.3", destResources);

		// do it again but only copy the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setHidden("4.0", project, true, IResource.DEPTH_INFINITE);
		try {
			folder.copy(destFolder.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("4.1", e);
		}
		assertExistsInWorkspace("4.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("4.3", new IResource[] {destFolder, destSubFile});
	}

	public void testMove() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// handles to the destination resources
		IProject destProject = root.getProject("MyOtherProject");
		IFolder destFolder = destProject.getFolder(folder.getName());
		IFile destFile = destProject.getFile(file.getName());
		IFile destSubFile = destFolder.getFile(subFile.getName());
		IResource[] destResources = new IResource[] {destProject, destFolder, destFile, destSubFile};
		ensureDoesNotExistInWorkspace(destResources);

		// set a folder to be hidden
		setHidden("1.0", folder, true, IResource.DEPTH_ZERO);
		// move the project
		int flags = IResource.FORCE;
		try {
			project.move(destProject.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		assertDoesNotExistInWorkspace("1.2", resources);
		assertExistsInWorkspace("1.3", destResources);

		// do it again and but just move the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		try {
			folder.move(destFolder.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}
		assertDoesNotExistInWorkspace("2.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.3", new IResource[] {destFolder, destSubFile});

		// set all the resources to be hidden
		// move the project
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		try {
			project.move(destProject.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}
		assertDoesNotExistInWorkspace("3.2", resources);
		assertExistsInWorkspace("3.3", destResources);

		// do it again but only move the folder
		ensureDoesNotExistInWorkspace(destResources);
		ensureExistsInWorkspace(resources, true);
		ensureExistsInWorkspace(destProject, true);
		setHidden("4.0", project, true, IResource.DEPTH_INFINITE);
		try {
			folder.move(destFolder.getFullPath(), flags, getMonitor());
		} catch (CoreException e) {
			fail("4.1", e);
		}
		assertDoesNotExistInWorkspace("4.2", new IResource[] {folder, subFile});
		assertExistsInWorkspace("4.3", new IResource[] {destFolder, destSubFile});
	}

	public void testDelete() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// default behavior with no hidden
		int flags = IResource.ALWAYS_DELETE_PROJECT_CONTENT | IResource.FORCE;
		// delete the project
		try {
			project.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertDoesNotExistInWorkspace("1.1", resources);
		// delete a file
		ensureExistsInWorkspace(resources, true);
		try {
			file.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}
		assertDoesNotExistInWorkspace("1.3", file);
		assertExistsInWorkspace("1.4", new IResource[] {project, folder, subFile});
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		try {
			folder.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("1.5", e);
		}
		assertDoesNotExistInWorkspace("1.6", new IResource[] {folder, subFile});
		assertExistsInWorkspace("1.7", new IResource[] {project, file});

		// set one child to be hidden
		ensureExistsInWorkspace(resources, true);
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		// delete the project
		try {
			project.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}
		assertDoesNotExistInWorkspace("2.2", resources);
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		setHidden("2.3", folder, true, IResource.DEPTH_ZERO);
		try {
			folder.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("2.4", e);
		}
		assertDoesNotExistInWorkspace("2.5", new IResource[] {folder, subFile});
		assertExistsInWorkspace("2.6", new IResource[] {project, file});

		// set all resources to be hidden
		ensureExistsInWorkspace(resources, true);
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		// delete the project
		try {
			project.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}
		assertDoesNotExistInWorkspace("3.2", resources);
		// delete a file
		ensureExistsInWorkspace(resources, true);
		setHidden("3.3", project, true, IResource.DEPTH_INFINITE);
		try {
			file.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("3.4", e);
		}
		assertDoesNotExistInWorkspace("3.5", file);
		assertExistsInWorkspace("3.6", new IResource[] {project, folder, subFile});
		// delete a folder
		ensureExistsInWorkspace(resources, true);
		setHidden("3.7", project, true, IResource.DEPTH_INFINITE);
		try {
			folder.delete(flags, getMonitor());
		} catch (CoreException e) {
			fail("3.8", e);
		}
		assertDoesNotExistInWorkspace("3.9", new IResource[] {folder, subFile});
		assertExistsInWorkspace("3.10", new IResource[] {project, file});
	}

	public void testDeltas() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		final IProject project = root.getProject(getUniqueString());
		final IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IFile description = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		final IResource[] resources = new IResource[] {project, folder, file, subFile};
		final ResourceDeltaVerifier listener = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					ensureExistsInWorkspace(resources, true);
				}
			};
			try {
				listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
				listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
				listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
				getWorkspace().run(body, getMonitor());
				waitForBuild();
				assertTrue("1.0." + listener.getMessage(), listener.isDeltaValid());
				ensureDoesNotExistInWorkspace(resources);
			} catch (CoreException e) {
				fail("1.1", e);
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set the folder to be hidden and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					ensureExistsInWorkspace(resources, true);
					setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
				}
			};
			try {
				listener.reset();
				listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
				listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
				listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
				getWorkspace().run(body, getMonitor());
				assertTrue("2.1." + listener.getMessage(), listener.isDeltaValid());
				ensureDoesNotExistInWorkspace(resources);
			} catch (CoreException e) {
				fail("2.2", e);
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}

		// set all resources to be hidden and do the same test
		getWorkspace().addResourceChangeListener(listener);
		try {
			IWorkspaceRunnable body = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					ensureExistsInWorkspace(resources, true);
					setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
				}
			};
			try {
				listener.reset();
				listener.addExpectedChange(resources, IResourceDelta.ADDED, IResource.NONE);
				listener.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
				listener.addExpectedChange(description, IResourceDelta.ADDED, IResource.NONE);
				getWorkspace().run(body, getMonitor());
				assertTrue("3.1." + listener.getMessage(), listener.isDeltaValid());
				ensureDoesNotExistInWorkspace(resources);
			} catch (CoreException e) {
				fail("3.2", e);
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
	}

	/**
	 * Resources which are marked as hidden resources return TRUE
	 * in all calls to #exists.
	 */
	public void testExists() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};
		ensureExistsInWorkspace(resources, true);

		// check to see if all the resources exist in the workspace tree.
		assertExistsInWorkspace("1.0", resources);

		// set a folder to be hidden
		setHidden("2.0", folder, true, IResource.DEPTH_ZERO);
		assertHidden("2.1", folder, true, IResource.DEPTH_ZERO);
		assertExistsInWorkspace("2.2", resources);

		// set all resources to be hidden
		setHidden("3.0", project, true, IResource.DEPTH_INFINITE);
		assertHidden("3.1", project, true, IResource.DEPTH_INFINITE);
		assertExistsInWorkspace("3.2", resources);
	}

	/**
	 * Test the set and get methods for hidden resources.
	 */
	public void testSetGet() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile subFile = folder.getFile("subfile.txt");
		IResource[] resources = new IResource[] {project, folder, file, subFile};

		// trying to set the value on non-existing resources will fail
		for (int i = 0; i < resources.length; i++) {
			try {
				resources[i].setHidden(true);
				fail("0.0." + resources[i].getFullPath());
			} catch (CoreException e) {
				// expected
			}
		}

		// create the resources
		ensureExistsInWorkspace(resources, true);

		// initial values should be false
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			assertTrue("1.0: " + resource.getFullPath(), !resource.isHidden());
		}

		// now set the values
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			try {
				resource.setHidden(true);
			} catch (CoreException e) {
				fail("2.0: " + resource.getFullPath(), e);
			}
		}

		// the values should be true for projects only
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			switch (resource.getType()) {
				case IResource.PROJECT :
				case IResource.FOLDER :
				case IResource.FILE :
					assertTrue("3.0: " + resource.getFullPath(), resource.isHidden());
					break;
				case IResource.ROOT :
					assertTrue("3.1: " + resource.getFullPath(), !resource.isHidden());
					break;
			}
		}

		// clear the values
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			try {
				resource.setHidden(false);
			} catch (CoreException e) {
				fail("4.0: " + resource.getFullPath(), e);
			}
		}

		// values should be false again
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			assertTrue("5.0: " + resource.getFullPath(), !resource.isHidden());
		}
	}

	/**
	 * Tests whether {@link IFile#create(java.io.InputStream, int, IProgressMonitor)},
	 * {@link IFolder#create(int, boolean, IProgressMonitor)} 
	 * and {@link IProject#create(IProjectDescription, int, IProgressMonitor) 
	 * handles {@link IResource#HIDDEN} flag properly.
	 */
	public void testCreateHiddenResources() {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");

		ensureExistsInWorkspace(project, true);

		try {
			folder.create(IResource.HIDDEN, true, getMonitor());
			file.create(getRandomContents(), IResource.HIDDEN, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertHidden("2.0", project, false, IResource.DEPTH_ZERO);
		assertHidden("3.0", folder, true, IResource.DEPTH_ZERO);
		assertHidden("4.0", file, true, IResource.DEPTH_ZERO);

		IProject project2 = getWorkspace().getRoot().getProject(getUniqueString());

		try {
			project2.create(null, IResource.HIDDEN, getMonitor());
			project2.open(getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertHidden("6.0", project2, true, IResource.DEPTH_ZERO);
	}
	
	protected void assertHidden(final String message, IResource root, final boolean value, int depth) {
		IResourceVisitor visitor = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				boolean expected = false;
				if (resource.getType() == IResource.PROJECT || resource.getType() == IResource.FILE || resource.getType() == IResource.FOLDER)
					expected = value;
				assertEquals(message + resource.getFullPath(), expected, resource.isHidden());
				return true;
			}
		};
		try {
			root.accept(visitor, depth, IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail(message + "resource.accept", e);
		}
	}

	protected void setHidden(final String message, IResource root, final boolean value, int depth) {
		IResourceVisitor visitor = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				try {
					resource.setHidden(value);
				} catch (CoreException e) {
					fail(message + resource.getFullPath(), e);
				}
				return true;
			}
		};
		try {
			root.accept(visitor, depth, IContainer.INCLUDE_HIDDEN);
		} catch (CoreException e) {
			fail(message + "resource.accept", e);
		}
	}
}