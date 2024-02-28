package org.eclipse.ui.internal.ide.commands;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 3.4
 *
 */
public class ZipExpander {

	public static void expandZip(IFile file, Shell shell) {
		try {
			URI zipURI = new URI("zip", null, "/", file.getLocationURI().toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
			IFolder link = file.getParent().getFolder(IPath.fromOSString(file.getName()));
			link.createLink(zipURI, IResource.REPLACE, null);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", "Error opening zip file"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	}
}
