/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

public class ASTCreator {

	public static final String CU_PROPERTY= "org.eclipse.jdt.ui.refactoring.cu"; //$NON-NLS-1$

	private ASTCreator() {
		//private
	}

	public static CompilationUnit createAST(ICompilationUnit cu, WorkingCopyOwner workingCopyOwner) {
		CompilationUnit cuNode= getCuNode(workingCopyOwner, cu);
		cuNode.setProperty(CU_PROPERTY, cu);
		return cuNode;
	}

	private static CompilationUnit getCuNode(WorkingCopyOwner workingCopyOwner, ICompilationUnit cu) {
		ASTParser p = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setSource(cu);
		p.setResolveBindings(true);
		p.setWorkingCopyOwner(workingCopyOwner);
		p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(cu));
		return (CompilationUnit) p.createAST(null);
	}

	public static ICompilationUnit getCu(ASTNode node) {
		Object property= node.getRoot().getProperty(CU_PROPERTY);
		if (property instanceof ICompilationUnit)
			return (ICompilationUnit)property;
		return null;
	}
}
