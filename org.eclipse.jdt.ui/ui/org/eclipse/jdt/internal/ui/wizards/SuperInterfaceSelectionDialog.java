/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class SuperInterfaceSelectionDialog extends TypeSelectionDialog {
	
	private static final int ADD_ID= IDialogConstants.CLIENT_ID + 1;
	
	private ListDialogField fList;
	private List fOldContent;
	
	public SuperInterfaceSelectionDialog(Shell parent, IRunnableContext context, ListDialogField list, IJavaProject p) {
		super(parent, context, createSearchScope(p), IJavaElementSearchConstants.CONSIDER_INTERFACES);
		fList= list;
		//to restore the content of the dialog field if the dialog is canceled
		fOldContent= fList.getElements(); 
	}

	/*
	 * @see Dialog#createButtonsForButtonBar
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ADD_ID, NewWizardMessages.getString("SuperInterfaceSelectionDialog.addButton.label"), true); //$NON-NLS-1$
		super.createButtonsForButtonBar(parent);
	}

	/*
	 * @see Dialog#cancelPressed
	 */
	protected void cancelPressed() {
		fList.setElements(fOldContent);
		super.cancelPressed();
	}
	
	/*
	 * @see Dialog#buttonPressed
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == ADD_ID){
			addSelectedInterface();
		}
		super.buttonPressed(buttonId);	
	}
	
	/*
	 * @see Dialog#okPressed
	 */
	protected void okPressed() {
		addSelectedInterface();
		super.okPressed();
	}
		
	private void addSelectedInterface(){
		Object ref= getLowerSelectedElement();
		if (ref instanceof TypeInfo)
			fList.addElement(((TypeInfo)ref).getFullyQualifiedName());
	}
	
	private static IJavaSearchScope createSearchScope(IJavaProject p) {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { p });
	}
	
	/*
	 * @see AbstractElementListSelectionDialog#handleDefaultSelected()
	 */
	protected void handleDefaultSelected() {
		if (validateCurrentSelection())
			buttonPressed(ADD_ID);
	}

}