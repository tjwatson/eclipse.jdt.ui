/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.GlobalBuildAction;
import org.eclipse.ui.dialogs.ListDialog;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSavePreferences;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * Helper to save dirty editors prior to starting a refactoring.
 *
 * @see PreferenceConstants#REFACTOR_SAVE_ALL_EDITORS
 * @since 3.5
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RefactoringSaveHelper {

	private boolean fFilesSaved;
	private final int fSaveMode;

	/**
	 * Save mode to save all dirty editors (always ask).
	 */
	public static final int SAVE_ALL_ALWAYS_ASK= IRefactoringSaveModes.SAVE_ALL_ALWAYS_ASK;

	/**
	 * Save mode to save all dirty editors.
	 */
	public static final int SAVE_ALL= IRefactoringSaveModes.SAVE_ALL;

	/**
	 * Save mode to not save any editors.
	 */
	public static final int SAVE_NOTHING= IRefactoringSaveModes.SAVE_NOTHING;

	/**
	 * Save mode to save all editors that are known to cause trouble for Java refactorings, e.g.
	 * editors on compilation units that are not in working copy mode.
	 */
	public static final int SAVE_REFACTORING= IRefactoringSaveModes.SAVE_REFACTORING;

	/**
	 * Creates a refactoring save helper with the given save mode.
	 *
	 * @param saveMode one of the SAVE_* constants
	 */
	public RefactoringSaveHelper(int saveMode) {
		Assert.isLegal(saveMode == SAVE_ALL_ALWAYS_ASK
				|| saveMode == SAVE_ALL
				|| saveMode == SAVE_NOTHING
				|| saveMode == SAVE_REFACTORING);
		fSaveMode= saveMode;
	}

	/**
	 * Saves all editors. Depending on the {@link PreferenceConstants#REFACTOR_SAVE_ALL_EDITORS}
	 * preference, the user is asked to save affected dirty editors.
	 *
	 * @param shell the parent shell for the confirmation dialog
	 * @return <code>true</code> if save was successful and refactoring can proceed;
	 * 		false if the refactoring must be cancelled
	 */
	public boolean saveEditors(Shell shell) {
		final IEditorPart[] dirtyEditors;
		switch (fSaveMode) {
			case SAVE_ALL_ALWAYS_ASK:
			case SAVE_ALL:
				dirtyEditors= EditorUtility.getDirtyEditors(true);
				break;

			case SAVE_REFACTORING:
				dirtyEditors= EditorUtility.getDirtyEditorsToSave(false); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=175495
				break;

			case SAVE_NOTHING:
				return true;

			default:
				throw new IllegalStateException(Integer.toString(fSaveMode));
		}
		if (dirtyEditors.length == 0)
			return true;
		if (! askSaveAllDirtyEditors(shell, dirtyEditors))
			return false;
		try {
			// Save isn't cancelable.
			boolean autoBuild= CoreUtility.setAutoBuilding(false);
			try {
				if (fSaveMode == SAVE_ALL_ALWAYS_ASK || fSaveMode == SAVE_ALL
						|| RefactoringSavePreferences.getSaveAllEditors()) {
					if (!JavaPlugin.getActiveWorkbenchWindow().getWorkbench().saveAllEditors(false))
						return false;
				} else {
					IRunnableWithProgress runnable= pm -> {
						int count= dirtyEditors.length;
						pm.beginTask("", count); //$NON-NLS-1$
						for (int i= 0; i < count; i++) {
							IEditorPart editor= dirtyEditors[i];
							editor.doSave(new SubProgressMonitor(pm, 1));
							if (pm.isCanceled())
								throw new InterruptedException();
						}
						pm.done();
					};
					try {
						PlatformUI.getWorkbench().getProgressService().runInUI(JavaPlugin.getActiveWorkbenchWindow(), runnable, null);
					} catch (InterruptedException e) {
						return false;
					} catch (InvocationTargetException e) {
						ExceptionHandler.handle(e, shell,
								RefactoringMessages.RefactoringStarter_saving, RefactoringMessages.RefactoringStarter_unexpected_exception);
						return false;
					}
				}
				fFilesSaved= true;
			} finally {
				CoreUtility.setAutoBuilding(autoBuild);
			}
			return true;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell,
				RefactoringMessages.RefactoringStarter_saving, RefactoringMessages.RefactoringStarter_unexpected_exception);
			return false;
		}
	}

	/**
	 * Triggers an incremental build if this save helper did save files before.
	 */
	public void triggerIncrementalBuild() {
		if (fFilesSaved && ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding()) {
			new GlobalBuildAction(JavaPlugin.getActiveWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}

	/**
	 * Returns whether this save helper did actually save any files.
	 *
	 * @return <code>true</code> iff files have been saved
	 */
	public boolean didSaveFiles() {
		return fFilesSaved;
	}

	private boolean askSaveAllDirtyEditors(Shell shell, IEditorPart[] dirtyEditors) {
		final boolean canSaveAutomatically= fSaveMode != SAVE_ALL_ALWAYS_ASK;
		if (canSaveAutomatically && RefactoringSavePreferences.getSaveAllEditors()) //must save everything
			return true;
		ListDialog dialog= new ListDialog(shell) {
			{
				setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL);
			}
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite) super.createDialogArea(parent);
				if (canSaveAutomatically) {
					final Button check= new Button(result, SWT.CHECK);
					check.setText(RefactoringMessages.RefactoringStarter_always_save);
					check.setSelection(RefactoringSavePreferences.getSaveAllEditors());
					check.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							RefactoringSavePreferences.setSaveAllEditors(check.getSelection());
						}
					});
					applyDialogFont(result);
				}
				return result;
			}
		};
		dialog.setTitle(RefactoringMessages.RefactoringStarter_save_all_resources);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage(RefactoringMessages.RefactoringStarter_must_save);
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setInput(Arrays.asList(dirtyEditors));
		return dialog.open() == Window.OK;
	}

	private ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			@Override
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
}
