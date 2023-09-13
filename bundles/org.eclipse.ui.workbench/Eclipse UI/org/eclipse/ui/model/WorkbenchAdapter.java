/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Fair Isaac Corporation <Hemant.Singh@Gmail.com> - Bug 326695
 *******************************************************************************/
package org.eclipse.ui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

/**
 * Abstract base class with basic implementations of the IWorkbenchAdapter
 * interface. Intended to be subclassed.
 *
 * @since 3.0
 */
public abstract class WorkbenchAdapter implements IWorkbenchAdapter, IWorkbenchAdapter2, IWorkbenchAdapter3 {
	/**
	 * The empty list of children.
	 */
	protected static final Object[] NO_CHILDREN = new Object[0];

	/**
	 * The default implementation of this <code>IWorkbenchAdapter</code> method
	 * returns the empty list. Subclasses may override.
	 */
	@Override
	public Object[] getChildren(Object object) {
		if (object instanceof IFile) {
			IFile archiveFile = (IFile) object;

			if (isArchive(archiveFile)) {
				List<IResource> children = new ArrayList<>();

				try (ZipFile zipFile = new ZipFile(archiveFile.getLocation().toFile())) {
					Enumeration<? extends ZipEntry> entries = zipFile.entries();

					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						entry.isDirectory();
						String entryName = entry.getName();
						IFile childFile = getChildFile(entryName, archiveFile);
						children.add(childFile);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return children.toArray();
			}
		}
		return NO_CHILDREN;
	}

	private boolean isArchive(IFile file) {
		String fileExtension = file.getFileExtension();
		return fileExtension != null
				&& (fileExtension.equalsIgnoreCase("zip") || fileExtension.equalsIgnoreCase("jar")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private IFile getChildFile(String entryName, IFile archiveFile) {
		IFile childFile = archiveFile.getProject().getFile(entryName);
		return childFile;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter</code> method
	 * returns <code>null</code>. Subclasses may override.
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter</code> method
	 * returns the empty string if the object is <code>null</code>, and the object's
	 * <code>toString</code> otherwise. Subclasses may override.
	 */
	@Override
	public String getLabel(Object object) {
		return object == null ? "" : object.toString(); //$NON-NLS-1$
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter</code> method
	 * returns <code>null</code>. Subclasses may override.
	 */
	@Override
	public Object getParent(Object object) {
		return null;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter2</code> method
	 * returns <code>null</code>. Subclasses may override.
	 */
	@Override
	public RGB getBackground(Object element) {
		return null;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter2</code> method
	 * returns <code>null</code>. Subclasses may override.
	 */
	@Override
	public RGB getForeground(Object element) {
		return null;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter2</code> method
	 * returns <code>null</code>. Subclasses may override.
	 */
	@Override
	public FontData getFont(Object element) {
		return null;
	}

	/**
	 * The default implementation of this <code>IWorkbenchAdapter3</code> method
	 * returns the {@link StyledString} which wraps the label of the element.
	 * Subclasses may override.
	 *
	 * @return Return the {@link StyledString} which wraps the label of the element.
	 *
	 * @since 3.7
	 */
	@Override
	public StyledString getStyledText(Object object) {
		return new StyledString(getLabel(object));
	}
}