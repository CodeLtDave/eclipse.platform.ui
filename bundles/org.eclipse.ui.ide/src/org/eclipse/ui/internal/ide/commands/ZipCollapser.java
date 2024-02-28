package org.eclipse.ui.internal.ide.commands;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
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
public class ZipCollapser {

	public static void collapseZip(IFolder folder, Shell shell) {
		try {
			URI zipURI = new URI(folder.getLocationURI().getQuery());
			// check if the zip file is physically stored below the folder in the workspace
			IFileStore parentStore = EFS.getStore(folder.getParent().getLocationURI());
			URI childURI = parentStore.getChild(folder.getName()).toURI();
			if (URIUtil.equals(zipURI, childURI)) {
				// the zip file is in the workspace so just delete the link
				// and refresh the parent to create the resource
				folder.delete(IResource.COLLAPSE, null);
				folder.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
			} else {
				// otherwise the zip file must be a linked resource
				IFile file = folder.getParent().getFile(IPath.fromOSString(folder.getName()));
				file.createLink(zipURI, IResource.REPLACE, null);
			}
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", "Error opening zip file"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	}
}
