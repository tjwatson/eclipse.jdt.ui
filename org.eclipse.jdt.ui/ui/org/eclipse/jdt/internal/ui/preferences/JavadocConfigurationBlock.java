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
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class JavadocConfigurationBlock {
	private static final String FILE_IMPORT_MASK= "*.jar;*.zip"; //$NON-NLS-1$
	private static final String ERROR_DIALOG_TITLE= "Error Dialog"; //$NON-NLS-1$

	private StringDialogField fURLField;
	private StringDialogField fArchiveField;
	private StringDialogField fArchivePathField;
	private URL fInitialURL;
	private SelectionButtonDialogField fValidateURLButton;
	private SelectionButtonDialogField fValidateArchiveButton;
	private SelectionButtonDialogField fBrowseFolder;
	private SelectionButtonDialogField fURLRadioButton;
	private SelectionButtonDialogField fArchiveRadioButton;
	private SelectionButtonDialogField fBrowseArchive;
	private SelectionButtonDialogField fBrowseArchivePath;
	private Shell fShell;
	private IStatusChangeListener fContext;
		
	private IStatus fURLStatus;
	private IStatus fArchiveStatus;
	private IStatus fArchivePathStatus;
	
	private URL fURLResult;
	private URL fArchiveURLResult;
	
	boolean fIsForSource;
	
	
	public JavadocConfigurationBlock(Shell shell,  IStatusChangeListener context, URL initURL, boolean forSource) {
		fShell= shell;
		fContext= context;
		fInitialURL= initURL;
		fIsForSource= forSource;
		
		JDocConfigurationAdapter adapter= new JDocConfigurationAdapter();
		
		if (!forSource) {
			fURLRadioButton= new SelectionButtonDialogField(SWT.RADIO);
			fURLRadioButton.setDialogFieldListener(adapter);
			fURLRadioButton.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.location.type.path.label")); //$NON-NLS-1$
		}
		
		fURLField= new StringDialogField();
		fURLField.setDialogFieldListener(adapter);
		fURLField.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.location.path.label")); //$NON-NLS-1$

		fBrowseFolder= new SelectionButtonDialogField(SWT.PUSH);
		fBrowseFolder.setDialogFieldListener(adapter);		
		fBrowseFolder.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.browse.button")); //$NON-NLS-1$

		fValidateURLButton= new SelectionButtonDialogField(SWT.PUSH);
		fValidateURLButton.setDialogFieldListener(adapter);		
		fValidateURLButton.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.validate.button")); //$NON-NLS-1$

		if (!forSource) {
			fArchiveRadioButton= new SelectionButtonDialogField(SWT.RADIO);
			fArchiveRadioButton.setDialogFieldListener(adapter);
			fArchiveRadioButton.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.location.type.jar.label")); //$NON-NLS-1$
	
			fArchiveField= new StringDialogField();
			fArchiveField.setDialogFieldListener(adapter);
			fArchiveField.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.location.jar.label")); //$NON-NLS-1$
	
			fBrowseArchive= new SelectionButtonDialogField(SWT.PUSH);
			fBrowseArchive.setDialogFieldListener(adapter);		
			fBrowseArchive.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.browse.button")); //$NON-NLS-1$
			
			fArchivePathField= new StringDialogField();
			fArchivePathField.setDialogFieldListener(adapter);
			fArchivePathField.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.jar.path.label")); //$NON-NLS-1$
			
			fBrowseArchivePath= new SelectionButtonDialogField(SWT.PUSH);
			fBrowseArchivePath.setDialogFieldListener(adapter);		
			fBrowseArchivePath.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.browse.button")); //$NON-NLS-1$
	
			fValidateArchiveButton= new SelectionButtonDialogField(SWT.PUSH);
			fValidateArchiveButton.setDialogFieldListener(adapter);		
			fValidateArchiveButton.setLabelText(PreferencesMessages.getString("JavadocConfigurationBlock.validate.button")); //$NON-NLS-1$
		}

		fURLStatus= new StatusInfo();
		fArchiveStatus= new StatusInfo();
		fArchivePathStatus= new StatusInfo();
		
		initializeSelections();
	}
	
	public Control createContents(Composite parent) {
		fShell= parent.getShell();
		
		PixelConverter converter= new PixelConverter(parent);
		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 3;
		topLayout.marginWidth= 0;
		topLayout.marginHeight= 0;
		topComp.setLayout(topLayout);

		// Add the first radio button for the path
		if (!fIsForSource) {
			fURLRadioButton.doFillIntoGrid(topComp, 3);
		}
	
		fURLField.doFillIntoGrid(topComp, 2);
		LayoutUtil.setWidthHint(fURLField.getTextControl(null), converter.convertWidthInCharsToPixels(50));
		LayoutUtil.setHorizontalGrabbing(fURLField.getTextControl(null));		

		fBrowseFolder.doFillIntoGrid(topComp, 1);
		
		DialogField.createEmptySpace(topComp, 2);			
		fValidateURLButton.doFillIntoGrid(topComp, 1);

		//DialogField.createEmptySpace(topComp, 3);	
		
		if (!fIsForSource) {
			// Add the second radio button for the jar/zip
			fArchiveRadioButton.doFillIntoGrid(topComp, 3);
	
			// Add the jar/zip field
			fArchiveField.doFillIntoGrid(topComp, 2);
			LayoutUtil.setWidthHint(fArchiveField.getTextControl(null), converter.convertWidthInCharsToPixels(50));
			LayoutUtil.setHorizontalGrabbing(fArchiveField.getTextControl(null));		

	
			fBrowseArchive.doFillIntoGrid(topComp, 1);
			
			// Add the path chooser for the jar/zip
			fArchivePathField.doFillIntoGrid(topComp, 2);
			LayoutUtil.setWidthHint(fArchivePathField.getTextControl(null), converter.convertWidthInCharsToPixels(50));
			LayoutUtil.setHorizontalGrabbing(fArchivePathField.getTextControl(null));	

			
			fBrowseArchivePath.doFillIntoGrid(topComp, 1);
			
			DialogField.createEmptySpace(topComp, 2);
			fValidateArchiveButton.doFillIntoGrid(topComp, 1);

			int indent= converter.convertWidthInCharsToPixels(2);	
			LayoutUtil.setHorizontalIndent(fArchiveField.getLabelControl(null), indent);
			LayoutUtil.setHorizontalIndent(fArchivePathField.getLabelControl(null), indent);
			LayoutUtil.setHorizontalIndent(fURLField.getLabelControl(null), indent);
			
			fURLRadioButton.attachDialogFields(new DialogField[] {fURLField,  fBrowseFolder, fValidateURLButton });
			fArchiveRadioButton.attachDialogFields(new DialogField[] {fArchiveField,  fBrowseArchive, fArchivePathField, fBrowseArchivePath, fValidateArchiveButton });
		}

		
		return topComp;
	}	
	
	private void initializeSelections() {
		String initialValue = fInitialURL != null ? fInitialURL.toExternalForm() : ""; //$NON-NLS-1$
		
		if (fIsForSource) {
			fURLField.setText(initialValue);
			return;
		}
		String prefix= JavaDocLocations.ARCHIVE_PREFIX;
		boolean isArchive= initialValue.startsWith(prefix); //$NON-NLS-1$
		
		fURLRadioButton.setSelection(!isArchive);
		fArchiveRadioButton.setSelection(isArchive);
		
		if (isArchive) {
			String jarPathStr;
			String insidePath= ""; //$NON-NLS-1$
			int excIndex= initialValue.indexOf('!');
			if (excIndex == -1) {
				jarPathStr= initialValue.substring(prefix.length());
			} else {
				jarPathStr= initialValue.substring(prefix.length(), excIndex);
				insidePath= initialValue.substring(excIndex + 1);
				if (insidePath.length() > 0 && insidePath.charAt(0) == '/') {
					insidePath= insidePath.substring(1);
				}
			}
			IPath jarPath= new Path(jarPathStr);
			fArchivePathField.setText(insidePath);
			fArchiveField.setText(jarPath.makeAbsolute().toOSString());
		} else {
			fURLField.setText(initialValue);
		}		
	}
		
	public void setFocus() {
		fURLField.postSetFocusOnDialogField(fShell.getDisplay());
	}
	
	public void performDefaults() {
		initializeSelections();
	}
	
	public URL getJavadocLocation() {
		if (fIsForSource || fURLRadioButton.isSelected()) {
			return fURLResult;
		}
		return fArchiveURLResult;
	}
		
	private class EntryValidator implements Runnable {

		private String fInvalidMessage= PreferencesMessages.getString("JavadocConfigurationBlock.InvalidLocation.message"); //$NON-NLS-1$
		private String fValidMessage= PreferencesMessages.getString("JavadocConfigurationBlock.ValidLocation.message"); //$NON-NLS-1$
		private String fTitle=  PreferencesMessages.getString("JavadocConfigurationBlock.MessageDialog.title"); //$NON-NLS-1$
		private String fUnable= PreferencesMessages.getString("JavadocConfigurationBlock.UnableToValidateLocation.message"); //$NON-NLS-1$
		public void run() {

			URL location= getJavadocLocation();
			if (location == null) {
				MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
				return;
			}

			try {
				String protocol = location.getProtocol();
				if (protocol.equals("http") || protocol.equals("jar")) { //$NON-NLS-1$ //$NON-NLS-2$
					validateURL(location);
				} else if (protocol.equals("file")) { //$NON-NLS-1$
					validateFile(location);
				} else {
					MessageDialog.openInformation(fShell, fTitle, fUnable); //$NON-NLS-1$
				}
			} catch (MalformedURLException e) {
				MessageDialog.openInformation(fShell, fTitle, fUnable); //$NON-NLS-1$
			}

		}
		
		public void spawnInBrowser(URL url) {
			OpenBrowserUtil.open(url, fShell.getDisplay(), fTitle);
		}

		private void validateFile(URL location) throws MalformedURLException {
			File folder = new File(location.getFile());
			if (folder.isDirectory()) {
				File indexFile= new File(folder, "index.html"); //$NON-NLS-1$
				if (indexFile.isFile()) {				
					File packageList= new File(folder, "package-list"); //$NON-NLS-1$
					if (packageList.exists()) {
						if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage)) { //$NON-NLS-1$
							spawnInBrowser(indexFile.toURL());
						}
						return;					
					}
				}
			}
			MessageDialog.openInformation(fShell, fTitle, fInvalidMessage); //$NON-NLS-1$
		}
		
		private void validateURL(URL location) throws MalformedURLException {
			IPath path= new Path(location.toExternalForm());
			IPath index = path.append("index.html"); //$NON-NLS-1$
			IPath packagelist = path.append("package-list"); //$NON-NLS-1$
			URL indexURL = new URL(index.toString());
			URL packagelistURL = new URL(packagelist.toString());

			InputStream in1= null;
			InputStream in2= null;
			try {
				in1= indexURL.openConnection().getInputStream();
				in2= packagelistURL.openConnection().getInputStream();

				if (MessageDialog.openConfirm(fShell, fTitle, fValidMessage))
					spawnInBrowser(indexURL);

			} catch (IOException e) {
				MessageDialog.openInformation(fShell, fTitle, fInvalidMessage);
			} finally {
				if (in1 != null) { try { in1.close(); } catch (IOException e) {} }
				if (in2 != null) { try { in2.close(); } catch (IOException e) {} }
			}				
		}
	}
	
	private class JDocConfigurationAdapter implements IDialogFieldListener {

		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			jdocDialogFieldChanged(field);
		}
	}


	private void jdocDialogFieldChanged(DialogField field) {
		if (field == fURLField) {
			fURLStatus= updateURLStatus();
			statusChanged();
		} else if (field == fArchiveField) {
			fArchiveStatus= updateArchiveStatus();
			statusChanged();
		} else if (field == fArchivePathField) {
			fArchivePathStatus= updateArchivePathStatus();
			statusChanged();
		} else if (field == fValidateURLButton || field == fValidateArchiveButton) {
			EntryValidator validator= new EntryValidator();
			BusyIndicator.showWhile(fShell.getDisplay(), validator);
		} else if (field == fBrowseFolder) {
			String url= chooseJavaDocFolder();
			if (url != null) {
				fURLField.setText(url);
			}
		} else if (field == fBrowseArchive) {
			String jarPath= chooseArchive();
			if (jarPath != null) {
				fArchiveField.setText(jarPath);
			}
		} else if (field == fBrowseArchivePath) {
			String archivePath= chooseArchivePath();
			if (archivePath != null) {
				fArchivePathField.setText(archivePath);
			}		
		} else if (field == fURLRadioButton || field == fArchiveRadioButton) {
			statusChanged();							
		}
	}
	
	private void statusChanged() {
		IStatus status;
		boolean isURL= fIsForSource || fURLRadioButton.isSelected();
		if (isURL) {
			status= fURLStatus;
		} else {
			status= StatusUtil.getMoreSevere(fArchiveStatus, fArchivePathStatus);
		}
		if (!fIsForSource) {
			fBrowseArchivePath.setEnabled(!isURL && fArchiveStatus.isOK() && fArchiveField.getText().length() > 0);
		}
		fContext.statusChanged(status);
	}


	private String chooseArchivePath() {
		final String[] res= new String[] { null };
		BusyIndicator.showWhile(fShell.getDisplay(), new Runnable() {
			public void run() {
				res[0]= internalChooseArchivePath();
			}
		});
		return res[0];
	}
		
		

	private String internalChooseArchivePath() {		
		ZipFile zipFile= null;
		try {
			zipFile= new ZipFile(fArchiveField.getText());
			ZipFileStructureProvider provider= new ZipFileStructureProvider(zipFile);
			
			ILabelProvider lp= new ZipDialogLabelProvider(provider);
			ZipDialogContentProvider cp= new ZipDialogContentProvider(provider);
			ViewerSorter sorter= new ViewerSorter() {};
						
			ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(fShell, lp, cp);
			dialog.setAllowMultiple(false);
			dialog.setValidator(new ZipDialogValidator());
			dialog.setTitle(PreferencesMessages.getString("JavadocConfigurationBlock.browse_jarorzip_path.title")); //$NON-NLS-1$
			dialog.setMessage(PreferencesMessages.getString("JavadocConfigurationBlock.location_in_jarorzip.message")); //$NON-NLS-1$
			dialog.setSorter(sorter);
			
			String init= fArchivePathField.getText();
			if (init.length() == 0) {
				init= "docs/api"; //$NON-NLS-1$
			}
			dialog.setInitialSelection(cp.findElement(new Path(init)));
			
			dialog.setInput(this);
			if (dialog.open() == Window.OK) {
				String name= provider.getFullPath(dialog.getFirstResult());
				return new Path(name).removeTrailingSeparator().toString();
			}
		} catch (IOException e) {
			JavaPlugin.log(e);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e1) {
					// ignore
				}
			}
		}
		return null;
	}

	private String chooseArchive() {
		FileDialog dialog= new FileDialog(fShell, SWT.OPEN);
		dialog.setFilterExtensions(new String[] { FILE_IMPORT_MASK });
		dialog.setText(PreferencesMessages.getString("JavadocConfigurationBlock.zipImportSource.title")); //$NON-NLS-1$

		String currentSourceString= fArchiveField.getText();
		int lastSeparatorIndex=	currentSourceString.lastIndexOf(File.separator);
		if (lastSeparatorIndex != -1)
			dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));

		return dialog.open();
	}
	
	/**
	 * Display an error dialog with the specified message.
	 *
	 * @param message the error message
	 */
	protected void displayErrorDialog(String message) {
		MessageDialog.openError(fShell, ERROR_DIALOG_TITLE, message); //$NON-NLS-1$
	}
		
	private String chooseJavaDocFolder() {
		String initPath= ""; //$NON-NLS-1$
		if (fURLResult != null && "file".equals(fURLResult.getProtocol())) { //$NON-NLS-1$
			initPath= (new File(fURLResult.getFile())).getPath();
		}
		DirectoryDialog dialog= new DirectoryDialog(fShell);
		dialog.setText(PreferencesMessages.getString("JavadocConfigurationBlock.javadocFolderDialog.label")); //$NON-NLS-1$
		dialog.setMessage(PreferencesMessages.getString("JavadocConfigurationBlock.javadocFolderDialog.message")); //$NON-NLS-1$
		dialog.setFilterPath(initPath);
		String result= dialog.open();
		if (result != null) {
			try {
				URL url= new File(result).toURL();
				return url.toExternalForm();
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}
		
	private IStatus updateURLStatus() {
		StatusInfo status= new StatusInfo();
		fURLResult= null;
		try {
			String jdocLocation= fURLField.getText();
			if (jdocLocation.length() == 0) {
				return status;
			}
			if (jdocLocation.length() > 0) {
				URL url= new URL(jdocLocation);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					if (url.getFile() == null) {
						status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
						return status;
					} else {
						File dir= new File(url.getFile());
						if (!dir.isDirectory()) {
							status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafolder")); //$NON-NLS-1$
							return status;
						}
						File packagesFile= new File(dir, "package-list"); //$NON-NLS-1$
						if (!packagesFile.exists()) {
							status.setWarning(PreferencesMessages.getString("JavadocConfigurationBlock.warning.packagelistnotfound")); //$NON-NLS-1$
							// only a warning, go on
						}						
					}
				}
				fURLResult= url;
			} 
		} catch (MalformedURLException e) {
			status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.MalformedURL.error"));  //$NON-NLS-1$
			return status;			
		}

		return status;
	}	
	
	private IStatus updateArchiveStatus() {
		try {
			fArchiveURLResult= null;
			
			StatusInfo status= new StatusInfo();
			String jdocLocation= fArchiveField.getText();
			if (jdocLocation.length() > 0)  {
				if (!Path.ROOT.isValidPath(jdocLocation)) {
					status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.invalidarchivepath")); //$NON-NLS-1$
					return status;	
				}
				File jarFile= new File(jdocLocation);
				if (jarFile.isDirectory())  {
					status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafile")); //$NON-NLS-1$
					return status;							
				}
				if (!jarFile.exists())  {
					status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.notafile")); //$NON-NLS-1$
					return status;													
				}
				IPath path= new Path(jdocLocation);
				if (!path.isAbsolute()) {
					status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.error.archivepathnotabsolute")); //$NON-NLS-1$
					return status;	
				}
				fArchiveURLResult= getArchiveURL();
			}
			return status;
		} catch (MalformedURLException e) {
			StatusInfo status= new StatusInfo();
			status.setError(e.getMessage());  //$NON-NLS-1$
			return status;
		}
	}
	
	private IStatus updateArchivePathStatus() {
		// no validation yet
		try {
			fArchiveURLResult= getArchiveURL();
		} catch (MalformedURLException e) {
			fArchiveURLResult= null;
			StatusInfo status= new StatusInfo();
			status.setError(e.getMessage());  //$NON-NLS-1$
			//status.setError(PreferencesMessages.getString("JavadocConfigurationBlock.MalformedURL.error"));  //$NON-NLS-1$
			return status;
		}
		return new StatusInfo();
	
	}
	
	
	private URL getArchiveURL() throws MalformedURLException {
		String jarLoc= fArchiveField.getText();
		String innerPath= fArchivePathField.getText().trim();
		
		StringBuffer buf= new StringBuffer();
		buf.append("jar:"); //$NON-NLS-1$
		buf.append(new File(jarLoc).toURL().toExternalForm());
		buf.append('!');
		if (innerPath.length() > 0) {
			if (innerPath.charAt(0) != '/') {
				buf.append('/');
			}
			buf.append(innerPath);
		} else {
			buf.append('/');
		}
		return new URL(buf.toString());
	}
	

	/**
	 * An adapter for presenting a zip file in a tree viewer.
	 */
	private static class ZipDialogContentProvider implements ITreeContentProvider {
	
		private ZipFileStructureProvider fProvider;
		
		public ZipDialogContentProvider(ZipFileStructureProvider provider) {
			fProvider= provider;
		}

		public Object findElement(IPath path) {
			String[] segments= path.segments();
			
			Object elem= fProvider.getRoot();
			for (int i= 0; i < segments.length && elem != null; i++) {
				List list= fProvider.getChildren(elem);
				String name= segments[i];
				elem= null;
				for (int k= 0; k < list.size(); k++) {
					Object curr= list.get(k);
					if (fProvider.isFolder(curr) && name.equals(fProvider.getLabel(curr))) {
						elem= curr;
						break;
					}
				}
			}
			return elem;
		}
		
		private Object recursiveFind(Object element, String name) {
			if (name.equals(fProvider.getLabel(element))) {
				return element;
			}
			List list= fProvider.getChildren(element);
			if (list != null) {
				for (int k= 0; k < list.size(); k++) {
					Object res= recursiveFind(list.get(k), name);
					if (res != null) {
						return res;
					}
				}				
			}
			return null;
		}
		
		public Object findFileByName(String name) {
			return recursiveFind(fProvider.getRoot(), name);
		}

		/* non java-doc
		 * @see ITreeContentProvider#inputChanged
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	
		/* non java-doc
		  * @see ITreeContentProvider#getParent
		  */
		public Object getParent(Object element) {
			if (element.equals(fProvider.getRoot())) {
				return null;
			}
			IPath path= new Path(fProvider.getFullPath(element));
			if (path.segmentCount() > 0) {
				return findElement(path.removeLastSegments(1));
			}
			return fProvider.getRoot();
		}
	
		/* non java-doc
		 * @see ITreeContentProvider#hasChildren
		 */
		public boolean hasChildren(Object element) {
			List list= fProvider.getChildren(element);
			if (list != null) {
				for (int i= 0; i < list.size(); i++) {
					if (fProvider.isFolder(list.get(i))) {
						return true;
					}
				}
			}
			return false;
		}
	
		/* non java-doc
		 * @see ITreeContentProvider#getChildren
		 */
		public Object[] getChildren(Object element) {
			List list= fProvider.getChildren(element);
			ArrayList res= new ArrayList();
			if (list != null) {
				for (int i= 0; i < list.size(); i++) {
					Object curr= list.get(i);
					if (fProvider.isFolder(curr)) {
						res.add(curr);
					}
				}
			}
			return res.toArray();
		}
	
		/* non java-doc
		 * @see ITreeContentProvider#getElements
		 */
		public Object[] getElements(Object element) {
			return new Object[] {fProvider.getRoot() };
		}
	
		/* non java-doc
		 * @see IContentProvider#dispose
		 */
		public void dispose() {
		}
	}
		
	private static class ZipDialogLabelProvider extends LabelProvider {
	
		private final Image IMG_JAR=
			JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
		private final Image IMG_FOLDER=
			PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	
		private ZipFileStructureProvider fProvider;
	
		public ZipDialogLabelProvider(ZipFileStructureProvider provider) {
			fProvider= provider;
		}
	
		public Image getImage(Object element) {
			if (element == fProvider.getRoot()) {
				return IMG_JAR;
			} else {
				return IMG_FOLDER;
			}
		}
	
		public String getText(Object element) {
			if (element == fProvider.getRoot()) {
				return fProvider.getZipFile().getName();
			}
			return fProvider.getLabel(element);
		}
	}
	
	private static class ZipDialogValidator implements ISelectionStatusValidator {
		public ZipDialogValidator() {
			super();
		}		
			
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			String message= ""; //$NON-NLS-1$
			return new StatusInfo(IStatus.INFO, message);
		}
	}	

}
