/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
 *     Patrick Ziegler - Migration from a JFace Action to a Command Handler,
 *                       in order to be used with the 'org.eclipse.ui.menus'
 *                       extension point.
 *******************************************************************************/
package org.eclipse.ui.internal.ide.commands;

import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExpandZipHandler extends AbstractHandler {

	public void expandZip(IFile file, Shell shell) {
		try {
			URI zipURI = new URI("zip", null, "/", file.getLocationURI().toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
			IFolder link = file.getParent().getFolder(IPath.fromOSString(file.getName()));
			link.createLink(zipURI, IResource.REPLACE, null);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", "Error opening zip file"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	}

	@Override
	public Object execute(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}

		Object element = ((IStructuredSelection) selection).getFirstElement();

		if (!(element instanceof IFile)) {
			return null;
		}

		expandZip((IFile) element, shell);
		return null;
	}

}
