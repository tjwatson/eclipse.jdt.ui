/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * @since 3.4
 */
public class NLSAccessorFieldRenameParticipant extends RenameParticipant {

	private IField fField;
	private String fNewName;
	private TextFileChange fChange;

	public NLSAccessorFieldRenameParticipant() {
	}

	@Override
	public String getName() {
		return NLSMessages.NLSAccessorFieldRenameParticipant_participantName;
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		if (pm == null)
			pm= new NullProgressMonitor();

		pm.beginTask("", 100); //$NON-NLS-1$
		try {
			ICompilationUnit unit= fField.getCompilationUnit();
			if (unit == null)
				return null;

			IType[] types= unit.getTypes();
			if (types.length > 1)
				return null;

			if (!isPotentialNLSAccessor(unit))
				return null;

			IStorage resourceBundle= NLSHintHelper.getResourceBundle(unit);
			if (!(resourceBundle instanceof IFile))
				return null;

			pm.worked(50);

			IPath propertyFilePath= resourceBundle.getFullPath();

			ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(propertyFilePath, LocationKind.IFILE, new SubProgressMonitor(pm, 25));

				IDocument document= manager.getTextFileBuffer(propertyFilePath, LocationKind.IFILE).getDocument();
				PropertyFileDocumentModel model= new PropertyFileDocumentModel(document);

				KeyValuePair oldPair= model.getKeyValuePair(fField.getElementName());
				if (oldPair == null)
					return null;

				String value= oldPair.getValue();
				KeyValuePair newPair= new KeyValuePair(fNewName, value);
				ReplaceEdit edit= model.replace(oldPair, newPair);
				if (edit == null)
					return null;

				fChange= new TextFileChange("", (IFile) resourceBundle); //$NON-NLS-1$
				fChange.setEdit(edit);

				String changeDescription= Messages.format(NLSMessages.NLSAccessorFieldRenameParticipant_changeDescription, new Object[] { fField.getElementName(), fNewName });
				fChange.addTextEditChangeGroup(new TextEditChangeGroup(fChange, new TextEditGroup(changeDescription, edit)));

				ResourceChangeChecker checker= context.getChecker(ResourceChangeChecker.class);
				IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
				deltaFactory.change((IFile) resourceBundle);
			} finally {
				manager.disconnect(propertyFilePath, LocationKind.IFILE, new SubProgressMonitor(pm, 25));
			}
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
			return RefactoringStatus.createErrorStatus(NLSMessages.NLSAccessorFieldRenameParticipant_error_description);
		} finally {
			pm.done();
		}

		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange;
	}

	@Override
	protected boolean initialize(Object element) {
		fField= (IField) element;
		return true;
	}

	@Override
	public boolean initialize(RefactoringProcessor processor, Object element, RefactoringArguments arguments) {
		fNewName= ((RenameArguments) arguments).getNewName();

		return super.initialize(processor, element, arguments);
	}

	/*
	 * Be conservative, for every unit this returns true an AST will to be created!
	 */
	private static boolean isPotentialNLSAccessor(ICompilationUnit unit) throws JavaModelException {
		IType type= unit.getTypes()[0];
		if (!type.exists())
			return false;

		IField bundleNameField= getBundleNameField(type.getFields());
		if (bundleNameField == null)
			return false;

		if (!importsOSGIUtil(unit))
			return false;

		for (IInitializer initializer : type.getInitializers()) {
			if (Modifier.isStatic(initializer.getFlags()))
				return true;
		}

		return false;
	}

	private static IField getBundleNameField(IField[] fields) {
		for (IField field : fields) {
			if ("BUNDLE_NAME".equals(field.getElementName())) { //$NON-NLS-1$
				return field;
			}
		}

		return null;
	}

	private static boolean importsOSGIUtil(ICompilationUnit unit) throws JavaModelException {
		for (IImportDeclaration importDecl : unit.getImports()) {
			if (importDecl.getElementName().startsWith("org.eclipse.osgi.util.")) { //$NON-NLS-1$
				return true;
			}
		}

		return false;
	}
}
