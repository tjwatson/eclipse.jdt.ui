/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Disables automatic builds.
 *
 * @since 3.1
 */
public class DisableAutoBuildTestSetup extends TestSetup {

	private boolean fWasAutobuilding;

	public DisableAutoBuildTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fWasAutobuilding= ResourceTestHelper.disableAutoBuilding();
	}

	protected void tearDown() throws Exception {
		if (fWasAutobuilding) {
			ResourceTestHelper.fullBuild();
			ResourceTestHelper.enableAutoBuilding();
		}
		super.tearDown();
	}
}
