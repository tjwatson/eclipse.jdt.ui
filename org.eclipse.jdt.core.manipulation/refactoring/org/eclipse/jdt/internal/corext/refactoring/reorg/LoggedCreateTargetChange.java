/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Change to create move targets during scripting of move refactorings.
 *
 * @since 3.3
 */
public final class LoggedCreateTargetChange extends ResourceChange {

	/** The queries */
	private final ICreateTargetQueries fQueries;

	/** The selection */
	private Object fSelection;

	/**
	 * Creates a new logged create target change.
	 *
	 * @param selection
	 *            the selection
	 * @param queries
	 *            the queries
	 */
	public LoggedCreateTargetChange(Object selection, ICreateTargetQueries queries) {
		fSelection= selection;
		fQueries= queries;
	}

	@Override
	protected IResource getModifiedResource() {
		return null;
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.LoggedCreateTargetChange_change_name;
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (fSelection instanceof IJavaElement) {
			final IJavaElement element= (IJavaElement) fSelection;
			if (!Checks.isAvailable(element))
				RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.RenameResourceChange_does_not_exist, JavaElementLabelsCore.getTextLabel(fSelection, JavaElementLabelsCore.ALL_DEFAULT)));
		} else if (fSelection instanceof IResource) {
			final IResource resource= (IResource) fSelection;
			if (!resource.exists())
				RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.RenameResourceChange_does_not_exist, JavaElementLabelsCore.getTextLabel(fSelection, JavaElementLabelsCore.ALL_DEFAULT)));
		}
		return new RefactoringStatus();
	}

	@Override
	public Change perform(IProgressMonitor monitor) throws CoreException {
		fQueries.createNewPackageQuery().getCreatedTarget(fSelection);
		return null;
	}
}
