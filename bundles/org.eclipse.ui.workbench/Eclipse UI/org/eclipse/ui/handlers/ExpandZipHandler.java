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
package org.eclipse.ui.handlers;

import java.net.URISyntaxException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ZipTransformer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

/**
 * This class represents a handler for expanding archive files.
 *
 * @since 3.132
 */
public class ExpandZipHandler extends AbstractHandler {

	/**
	 * Executes the handler action, which involves expanding an archive file
	 * selected by the user.
	 * 
	 * @param event The event triggering the execution of this handler.
	 */
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
		try {
			ZipTransformer.expandZip((IFile) element);
		} catch (URISyntaxException | CoreException e) {
			MessageDialog.openError(shell, "Error opening zip file", e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}
}
