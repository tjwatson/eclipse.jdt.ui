/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.core;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.testplugin.JavaProjectHelper;


public class ProjectTestSetup extends TestSetup {

	public static final String PROJECT_NAME= "TestSetupProject";
	
	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		return JavaCore.create(project);
	}
	
	
	private IJavaProject fJProject;
	private long fTimeCreated;
	
	public ProjectTestSetup(Test test) {
		super(test);
	}
	
	/* (non-Javadoc)
	 * @see junit.extensions.TestSetup#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		fTimeCreated= System.currentTimeMillis();
		
		IJavaProject project= getProject();
		if (project.exists()) { // can already be created 
			return;
		}
		
		fJProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPath[] rtJarPath= JavaProjectHelper.findRtJar();
		assertTrue("rt not found", rtJarPath != null);		
		
		IClasspathEntry cpe= JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true);
		JavaProjectHelper.addToClasspath(fJProject, cpe);		
	}

	protected void tearDown() throws Exception {
		if (fJProject != null) {
			JavaProjectHelper.delete(fJProject);
		}
		
		long taken= System.currentTimeMillis() -fTimeCreated;
		System.out.println(getTest().getClass().getName()+ ": " + taken);
	}

}
