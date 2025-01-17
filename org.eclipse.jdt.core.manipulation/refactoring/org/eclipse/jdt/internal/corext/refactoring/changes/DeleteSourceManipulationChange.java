/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.undo.snapshot.IResourceSnapshot;
import org.eclipse.core.resources.undo.snapshot.ResourceSnapshotFactory;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Caveat: undo of package fragment deletes is provided by a wrapping
 * UndoablePackageDeleteChange. This change returns a NullChange as undo for package fragments.
 */
public class DeleteSourceManipulationChange extends AbstractDeleteChange {

	private final String fHandle;

	public DeleteSourceManipulationChange(ISourceManipulation sm, boolean isExecuteChange) {
		Assert.isNotNull(sm);
		fHandle= getJavaElement(sm).getHandleIdentifier();

		if (isExecuteChange) {
			if (sm instanceof ICompilationUnit) {
				// don't check anything in this case. We have a warning dialog
				// already presented to the user that the file is dirty.
				setValidationMethod(VALIDATE_DEFAULT);
			} else {
				setValidationMethod(VALIDATE_NOT_DIRTY);
			}
		} else {
			setValidationMethod(VALIDATE_NOT_DIRTY | VALIDATE_NOT_READ_ONLY);
		}
	}

	/*
	 * @see IChange#getName()
	 */
	@Override
	public String getName() {
		IJavaElement javaElement= getJavaElement(getSourceManipulation());
		return Messages.format(RefactoringCoreMessages.DeleteSourceManipulationChange_0, JavaElementLabelsCore.getElementLabel(javaElement, JavaElementLabelsCore.ALL_DEFAULT));
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	@Override
	public Object getModifiedElement() {
		return JavaCore.create(fHandle);
	}

	@Override
	protected IResource getModifiedResource() {
		IJavaElement elem= JavaCore.create(fHandle);
		if (elem != null) {
			return elem.getResource();
		}
		return null;
	}

	/*
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	@Override
	protected Change doDelete(IProgressMonitor pm) throws CoreException {
		ISourceManipulation element= getSourceManipulation();
		// we have to save dirty compilation units before deleting them. Otherwise
		// we will end up showing ghost compilation units in the package explorer
		// since the primary working copy still exists.
		if (element instanceof ICompilationUnit) {
			pm.beginTask("", 2); //$NON-NLS-1$
			ICompilationUnit unit= (ICompilationUnit)element;
			saveCUnitIfNeeded(unit, SubMonitor.convert(pm, 1));

			IResource resource= unit.getResource();
			IResourceSnapshot<IResource> resourceDescription = ResourceSnapshotFactory.fromResource(resource);
			element.delete(false, SubMonitor.convert(pm, 1));
			resourceDescription.recordStateFromHistory(resource, SubMonitor.convert(pm, 1));
			return new UndoDeleteResourceChange(resourceDescription);

		} else if (element instanceof IPackageFragment) {
			ICompilationUnit[] units= ((IPackageFragment)element).getCompilationUnits();
			pm.beginTask("", units.length + 1); //$NON-NLS-1$
			for (ICompilationUnit unit : units) {
				// fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=66835
				saveCUnitIfNeeded(unit, SubMonitor.convert(pm, 1));
			}
			element.delete(false, SubMonitor.convert(pm, 1));
			return new NullChange(); // caveat: real undo implemented by UndoablePackageDeleteChange

		} else {
			element.delete(false, pm);
			return null; //should not happen
		}
	}

	private ISourceManipulation getSourceManipulation() {
		return (ISourceManipulation) getModifiedElement();
	}

	private static IJavaElement getJavaElement(ISourceManipulation sm) {
		//all known ISourceManipulations are IJavaElements
		return (IJavaElement)sm;
	}

	private static void saveCUnitIfNeeded(ICompilationUnit unit, IProgressMonitor pm) throws CoreException {
		saveFileIfNeeded((IFile)unit.getResource(), pm);
	}
}

