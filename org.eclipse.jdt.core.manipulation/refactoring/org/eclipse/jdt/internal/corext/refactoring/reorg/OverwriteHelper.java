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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

class OverwriteHelper {
	private Object fDestination;
	private IFile[] fFiles= new IFile[0];
	private IFolder[] fFolders= new IFolder[0];
	private ICompilationUnit[] fCus= new ICompilationUnit[0];
	private IPackageFragmentRoot[] fRoots= new IPackageFragmentRoot[0];
	private IPackageFragment[] fPackageFragments= new IPackageFragment[0];

	public void setFiles(IFile[] files) {
		Assert.isNotNull(files);
		fFiles= files;
	}

	public void setFolders(IFolder[] folders) {
		Assert.isNotNull(folders);
		fFolders= folders;
	}

	public void setCus(ICompilationUnit[] cus) {
		Assert.isNotNull(cus);
		fCus= cus;
	}

	public void setPackageFragmentRoots(IPackageFragmentRoot[] roots) {
		Assert.isNotNull(roots);
		fRoots= roots;
	}

	public void setPackages(IPackageFragment[] fragments) {
		Assert.isNotNull(fragments);
		fPackageFragments= fragments;
	}

	public IFile[] getFilesWithoutUnconfirmedOnes() {
		return fFiles;
	}

	public IFolder[] getFoldersWithoutUnconfirmedOnes() {
		return fFolders;
	}

	public ICompilationUnit[] getCusWithoutUnconfirmedOnes() {
		return fCus;
	}

	public IPackageFragmentRoot[] getPackageFragmentRootsWithoutUnconfirmedOnes() {
		return fRoots;
	}

	public IPackageFragment[] getPackagesWithoutUnconfirmedOnes() {
		return fPackageFragments;
	}

	public void confirmOverwriting(IReorgQueries reorgQueries, Object destination) {
		Assert.isNotNull(destination);
		Assert.isNotNull(reorgQueries);
		fDestination= destination;
		confirmOverwritting(reorgQueries);
	}

	private void confirmOverwritting(IReorgQueries reorgQueries) {
		IConfirmQuery overwriteQuery= reorgQueries.createYesYesToAllNoNoToAllQuery(RefactoringCoreMessages.OverwriteHelper_0, true, IReorgQueries.CONFIRM_OVERWRITING);
		IConfirmQuery skipQuery= reorgQueries.createSkipQuery(RefactoringCoreMessages.OverwriteHelper_2, IReorgQueries.CONFIRM_SKIPPING);
		confirmFileOverwritting(overwriteQuery, skipQuery);
		confirmFolderOverwritting(skipQuery);
		confirmCuOverwritting(overwriteQuery);
		confirmPackageFragmentRootOverwritting(skipQuery, overwriteQuery);
		confirmPackageOverwritting(overwriteQuery);
	}

	private void confirmPackageFragmentRootOverwritting(IConfirmQuery skipQuery, IConfirmQuery overwriteQuery) {
		List<IPackageFragmentRoot> toNotOverwrite= new ArrayList<>(1);
		for (IPackageFragmentRoot root : fRoots) {
			if (canOverwrite(root)) {
				if (root.getResource() instanceof IContainer) {
					if (!skip(JavaElementLabelsCore.getElementLabel(root, JavaElementLabelsCore.ALL_DEFAULT), skipQuery))
						toNotOverwrite.add(root);
				} else {
					if (!overwrite(root.getResource(), overwriteQuery))
						toNotOverwrite.add(root);
				}
			}
		}
		IPackageFragmentRoot[] roots= toNotOverwrite.toArray(new IPackageFragmentRoot[toNotOverwrite.size()]);
		fRoots= ArrayTypeConverter.toPackageFragmentRootArray(ReorgUtilsCore.setMinus(fRoots, roots));
	}

	private void confirmCuOverwritting(IConfirmQuery overwriteQuery) {
		List<ICompilationUnit> cusToNotOverwrite= new ArrayList<>(1);
		for (ICompilationUnit cu : fCus) {
			if (canOverwrite(cu) && ! overwrite(cu, overwriteQuery))
				cusToNotOverwrite.add(cu);
		}
		ICompilationUnit[] cus= cusToNotOverwrite.toArray(new ICompilationUnit[cusToNotOverwrite.size()]);
		fCus= ArrayTypeConverter.toCuArray(ReorgUtilsCore.setMinus(fCus, cus));
	}

	private void confirmFolderOverwritting(IConfirmQuery overwriteQuery) {
		List<IFolder> foldersToNotOverwrite= new ArrayList<>(1);
		for (IFolder folder : fFolders) {
			if (willOverwrite(folder) && ! skip(BasicElementLabels.getResourceName(folder), overwriteQuery))
				foldersToNotOverwrite.add(folder);
		}
		IFolder[] folders= foldersToNotOverwrite.toArray(new IFolder[foldersToNotOverwrite.size()]);
		fFolders= ArrayTypeConverter.toFolderArray(ReorgUtilsCore.setMinus(fFolders, folders));
	}

	private void confirmFileOverwritting(IConfirmQuery overwriteQuery, IConfirmQuery skipQuery) {
		List<IFile> filesToNotOverwrite= new ArrayList<>(1);
		for (IFile file : fFiles) {
			if (willOverwrite(file)) {
				IContainer destination= (IContainer) ResourceUtil.getResource(fDestination);
				if (ParentChecker.isDescendantOf(file, destination.findMember(file.getName()))) {
					if (!skip(BasicElementLabels.getResourceName(file), skipQuery)) {
						filesToNotOverwrite.add(file);
					}
				} else if (!overwrite(file, overwriteQuery)) {
					filesToNotOverwrite.add(file);
				}
			}
		}
		IFile[] files= filesToNotOverwrite.toArray(new IFile[filesToNotOverwrite.size()]);
		fFiles= ArrayTypeConverter.toFileArray(ReorgUtilsCore.setMinus(fFiles, files));
	}

	private void confirmPackageOverwritting(IConfirmQuery overwriteQuery){
		List<IPackageFragment> toNotOverwrite= new ArrayList<>(1);
		for (IPackageFragment pack : fPackageFragments) {
			if (canOverwrite(pack) && ! overwrite(pack, overwriteQuery))
				toNotOverwrite.add(pack);
		}
		IPackageFragment[] packages= toNotOverwrite.toArray(new IPackageFragment[toNotOverwrite.size()]);
		fPackageFragments= ArrayTypeConverter.toPackageArray(ReorgUtilsCore.setMinus(fPackageFragments, packages));
	}

	private boolean canOverwrite(IPackageFragment pack) {
		if (fDestination instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot destination= (IPackageFragmentRoot)fDestination;
			return ! destination.equals(pack.getParent()) && destination.getPackageFragment(pack.getElementName()).exists();
		} else {
			return willOverwrite(pack.getResource());
		}
	}

	/*
	 * Will resource override a member of destination?
	 */
	private boolean willOverwrite(IResource resource) {
		if (resource == null)
			return false;

		IResource destinationResource= ResourceUtil.getResource(fDestination);
		if (destinationResource.equals(resource.getParent()))
			return false;

		if (destinationResource instanceof IContainer) {
			IContainer container= (IContainer)destinationResource;
			IResource member=  container.findMember(resource.getName());
			if (member == null || !member.exists())
				return false;

			return true;
		}
		return false;
	}

	private boolean canOverwrite(IPackageFragmentRoot root) {
		if (fDestination instanceof IJavaProject) {
			IJavaProject destination= (IJavaProject)fDestination;
			IFolder conflict= destination.getProject().getFolder(root.getElementName());
			try {
				return !destination.equals(root.getParent()) && conflict.exists() &&  conflict.members().length > 0;
			} catch (CoreException e) {
				return true;
			}
		} else {
			return willOverwrite(root.getResource());
		}
	}

	private boolean canOverwrite(ICompilationUnit cu) {
		if (fDestination instanceof IPackageFragment){
			IPackageFragment destination= (IPackageFragment)fDestination;
			return ! destination.equals(cu.getParent()) && destination.getCompilationUnit(cu.getElementName()).exists();
		} else {
			return willOverwrite(ReorgUtilsCore.getResource(cu));
		}
	}

	private static boolean overwrite(IResource resource, IConfirmQuery overwriteQuery){
		return overwrite(BasicElementLabels.getResourceName(resource), overwriteQuery);
	}

	private static boolean overwrite(IJavaElement element, IConfirmQuery overwriteQuery){
		return overwrite(JavaElementLabelsCore.getElementLabel(element, JavaElementLabelsCore.ALL_DEFAULT), overwriteQuery);
	}

	private static boolean overwrite(String name, IConfirmQuery overwriteQuery){
		String question= Messages.format(RefactoringCoreMessages.OverwriteHelper_1, BasicElementLabels.getJavaElementName(name));
		return overwriteQuery.confirm(question);
	}
	private static boolean skip(String name, IConfirmQuery overwriteQuery){
		String question= Messages.format(RefactoringCoreMessages.OverwriteHelper_3, BasicElementLabels.getJavaElementName(name));
		return overwriteQuery.confirm(question);
	}
}
