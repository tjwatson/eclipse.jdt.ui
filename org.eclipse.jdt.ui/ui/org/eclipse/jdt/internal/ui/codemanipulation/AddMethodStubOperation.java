/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

/**
 * Add method stubs to a type (the parent type)
 * Methods are added without checking if they already exist (will result in duplicated methods)
 * If the parent type is open in an editor, be sure to pass over its working copy.
 */
public class AddMethodStubOperation extends WorkspaceModifyOperation {
	
	private IType fType;
	private IMethod[] fInheritedMethods;
	private IMethod[] fCreatedMethods;
	private boolean fDoSave;
	
	public AddMethodStubOperation(IType type, IMethod[] inheritedMethods, boolean save) {
		super();
		fType= type;
		fInheritedMethods= inheritedMethods;
		fCreatedMethods= null;
		fDoSave= save;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(CodeManipulationMessages.getString("AddMethodStubOperation.description"), fInheritedMethods.length + 1); //$NON-NLS-1$
			
			IMethod[] existingMethods= fType.getMethods();
			ArrayList createdMethods= new ArrayList();
			ImportsStructure imports= new ImportsStructure(fType.getCompilationUnit());
			
			String lineDelim= StubUtility.getLineDelimiterUsed(fType);
			int indent= StubUtility.getIndentUsed(fType) + 1;			
			
			for (int i= 0; i < fInheritedMethods.length; i++) {
				IMethod inheritedMethod= fInheritedMethods[i];
				String content= StubUtility.genStub(fType, inheritedMethod, imports);
				String formattedContent= StubUtility.codeFormat(content, indent, lineDelim) + lineDelim;
				IMethod newMethod= fType.createMethod(formattedContent, null, true, null);
				createdMethods.add(newMethod);
				monitor.worked(1);
			}
			
			int nCreated= createdMethods.size();
			if (nCreated > 0) {
				imports.create(fDoSave, null);
				monitor.worked(1);
				fCreatedMethods= new IMethod[nCreated];
				createdMethods.toArray(fCreatedMethods);
			}
		} finally {
			monitor.done();
		}
	}
	
	public IMethod[] getCreatedMethods() {
		return fCreatedMethods;
	}
	
		
}
