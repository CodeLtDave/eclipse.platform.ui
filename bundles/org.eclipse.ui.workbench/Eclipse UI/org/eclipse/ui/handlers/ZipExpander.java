package org.eclipse.ui.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @since 3.132
 *
 */
public class ZipExpander {
	public static void expandZip(IFile file) throws URISyntaxException, CoreException {
			URI zipURI = new URI("zip", null, "/", file.getLocationURI().toString(), null); //$NON-NLS-1$ //$NON-NLS-2$
			IFolder link = file.getParent().getFolder(IPath.fromOSString(file.getName()));
			link.createLink(zipURI, IResource.REPLACE, null);
	}
}
