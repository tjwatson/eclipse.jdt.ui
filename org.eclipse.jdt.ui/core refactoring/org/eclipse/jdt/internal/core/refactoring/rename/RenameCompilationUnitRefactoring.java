/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;


public class RenameCompilationUnitRefactoring extends Refactoring implements ITextUpdatingRefactoring{

	private String fNewName;
	private RenameTypeRefactoring fRenameTypeRefactoring;
	private boolean fWillRenameType;
	private ICompilationUnit fCu;
	
	public RenameCompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		Assert.isNotNull(cu);
		Assert.isNotNull(changeCreator);
		fCu= cu;
		computeRenameTypeRefactoring(changeCreator);
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}

		
	/* non javadoc
	 * see Refactoring#setUnsavedFileList
	 */
	public void setUnsavedFiles(IFile[] files){
		super.setUnsavedFiles(files);
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUnsavedFiles(files);
	}
		
	/* non java-doc
	 * @see IRenameRefactoring#setNewName(String)
	 * @param newName 'java' must be included
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
		if (fWillRenameType)
			fRenameTypeRefactoring.setNewName(removeFileNameExtension(newName));
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getNewName()
	*/
	public String getNewName(){
		return fNewName;
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkCompilationUnitName(fNewName));
		if (fWillRenameType)
			result.merge(fRenameTypeRefactoring.checkNewName());
		if (Checks.isAlreadyNamed(fCu, fNewName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameCompilationUnitRefactoring.same_name"));	 //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fCu.getElementName();
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.name",  //$NON-NLS-1$
															new String[]{fCu.getElementName(), fNewName});
	}

	/*
	 * @see ITextUpdatingRefactoring#canEnableTextUpdating()
	 */
	public boolean canEnableTextUpdating() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.canEnableUpdateReferences();
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateJavaDoc()
	 */
	public boolean getUpdateJavaDoc() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.getUpdateJavaDoc();
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateComments()
	 */
	public boolean getUpdateComments() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.getUpdateComments();
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateStrings()
	 */
	public boolean getUpdateStrings() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.getUpdateStrings();
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateJavaDoc(boolean)
	 */
	public void setUpdateJavaDoc(boolean update) {
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUpdateJavaDoc(update);
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateComments(boolean)
	 */
	public void setUpdateComments(boolean update) {
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUpdateComments(update);
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateStrings(boolean)
	 */
	public void setUpdateStrings(boolean update) {
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUpdateStrings(update);
	}

	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		if (fRenameTypeRefactoring == null)
			return false;
		return fRenameTypeRefactoring.canEnableUpdateReferences();
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean update) {
		if (fRenameTypeRefactoring != null)
			fRenameTypeRefactoring.setUpdateReferences(update);
	}

	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		if (fRenameTypeRefactoring == null)
			return false;

		return fRenameTypeRefactoring.getUpdateReferences();		
	}

	//--- preconditions
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		ICompilationUnit cu= fCu;
		if (! cu.exists())
			return RefactoringStatus.createFatalErrorStatus(""); //$NON-NLS-1$
		
		if (cu.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(""); //$NON-NLS-1$
		
		if (mustCancelRenamingType())
			fWillRenameType= false;
		
		if (fWillRenameType)
			return fRenameTypeRefactoring.checkPreactivation();
		else	
			return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		if (mustCancelRenamingType()){
			Assert.isTrue(! fWillRenameType);
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", fCu.getElementName())); //$NON-NLS-1$
		}	
		 
		// we purposely do not check activation of the renameTypeRefactoring here. 
		return new RefactoringStatus();
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fWillRenameType && (!fCu.isStructureKnown())){
				RefactoringStatus result1= new RefactoringStatus();
				
				RefactoringStatus result2= new RefactoringStatus();
				result2.merge(Checks.checkCompilationUnitNewName(fCu, fNewName));
				if (result2.hasFatalError())
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed_1", fCu.getElementName())); //$NON-NLS-1$
				else 
					result1.addError(RefactoringCoreMessages.getFormattedString("RenameCompilationUnitRefactoring.not_parsed", fCu.getElementName())); //$NON-NLS-1$
				result1.merge(result2);			
			}	
		
			if (fWillRenameType)
				return fRenameTypeRefactoring.checkInput(pm);
			else
				return Checks.checkCompilationUnitNewName(fCu, removeFileNameExtension(fNewName));
		} finally{
			pm.done();
		}		
	}
	
	private void computeRenameTypeRefactoring(ITextBufferChangeCreator changeCreator){
		if (getSimpleCUName().indexOf(".") != -1){ //$NON-NLS-1$
			fRenameTypeRefactoring= null;
			fWillRenameType= false;
			return;
		}
		IType type= fCu.getType(getSimpleCUName());
		if (type.exists())
			fRenameTypeRefactoring= new RenameTypeRefactoring(changeCreator, type);
		else
			fRenameTypeRefactoring= null;
		fWillRenameType= (fRenameTypeRefactoring != null);	
	}
	
	private boolean mustCancelRenamingType() throws JavaModelException {
		return (fRenameTypeRefactoring != null) && (! fCu.isStructureKnown());
	}
	
	private String getSimpleCUName(){
		return removeFileNameExtension(fCu.getElementName());
	}
	
	/**
	 * Removes the extension (whatever comes after the last '.') from the given file name.
	 */
	private static String removeFileNameExtension(String fileName) {
		if (fileName.lastIndexOf(".") == -1) //$NON-NLS-1$
			return fileName;
		return fileName.substring(0, fileName.lastIndexOf(".")); //$NON-NLS-1$
	}
	
	//--- changes
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		//renaming the file is taken care of in renameTypeRefactoring
		if (fWillRenameType)
			return fRenameTypeRefactoring.createChange(pm);
	
		return new RenameCompilationUnitChange(fCu, fNewName);
	}
}
