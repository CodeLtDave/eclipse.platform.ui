/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

package org.eclipse.search.ui.text;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentType;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.WorkingSetComparator;
import org.eclipse.search.internal.ui.text.BasicElementLabels;
import org.eclipse.search.internal.ui.util.FileTypeEditor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A text search scope used by the file search dialog. Additionally to roots it
 * allows to define file name patterns and exclude all derived resources.
 *
 * <p>
 * Clients should not instantiate or subclass this class.
 * </p>
 * 
 * @since 3.2
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FileTextSearchScope extends TextSearchScope {

	private static final boolean IS_CASE_SENSITIVE_FILESYSTEM = !new File("Temp").equals(new File("temp")); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Returns a scope for the workspace. The created scope contains all
	 * resources in the workspace that match the given file name patterns.
	 * Depending on <code>includeDerived</code>, derived resources or resources
	 * inside a derived container are part of the scope or not.
	 *
	 * @param fileNamePatterns
	 *            file name pattern that all files have to match
	 *            <code>null</code> to include all file names.
	 * @param includeDerived
	 *            defines if derived files and files inside derived containers
	 *            are included in the scope.
	 * @return a scope containing all files in the workspace that match the
	 *         given file name patterns.
	 */
	// public static FileTextSearchScope newWorkspaceScope(String[]
	// fileNamePatterns, boolean includeDerived) {
	// return new FileTextSearchScope(SearchMessages.WorkspaceScope,
	// new IResource[] { ResourcesPlugin.getWorkspace().getRoot() }, null,
	// fileNamePatterns, includeDerived);
	// }

	/**
	 * Returns a scope for the given root resources. The created scope contains
	 * all root resources and their children that match the given file name
	 * patterns. Depending on <code>includeDerived</code>, derived resources or
	 * resources inside a derived container are part of the scope or not.
	 *
	 * @param roots
	 *            the roots resources defining the scope.
	 * @param fileNamePatterns
	 *            file name pattern that all files have to match
	 *            <code>null</code> to include all file names.
	 * @param includeDerived
	 *            defines if derived files and files inside derived containers
	 *            are included in the scope.
	 * @return a scope containing the resources and its children if they match
	 *         the given file name patterns.
	 */
	public static FileTextSearchScope newSearchScope(IResource[] roots, String[] fileNamePatterns,
			boolean includeDerived) {
		roots = removeRedundantEntries(roots, includeDerived);

		String description;
		if (roots.length == 0) {
			description = SearchMessages.FileTextSearchScope_scope_empty;
		} else if (roots.length == 1) {
			String label = SearchMessages.FileTextSearchScope_scope_single;
			description = Messages.format(label, roots[0].getName());
		} else if (roots.length == 2) {
			String label = SearchMessages.FileTextSearchScope_scope_double;
			description = Messages.format(label, new String[] { roots[0].getName(), roots[1].getName() });
		} else {
			String label = SearchMessages.FileTextSearchScope_scope_multiple;
			description = Messages.format(label, new String[] { roots[0].getName(), roots[1].getName() });
		}
		return new FileTextSearchScope(description, roots, null, fileNamePatterns, includeDerived);
	}

	/**
	 * Returns a scope for the given working sets. The created scope contains
	 * all resources in the working sets that match the given file name
	 * patterns. Depending on <code>includeDerived</code>, derived resources or
	 * resources inside a derived container are part of the scope or not.
	 *
	 * @param workingSets
	 *            the working sets defining the scope.
	 * @param fileNamePatterns
	 *            file name pattern that all files have to match
	 *            <code>null</code> to include all file names.
	 * @param includeDerived
	 *            defines if derived files and files inside derived containers
	 *            are included in the scope.
	 * @return a scope containing the resources in the working set if they match
	 *         the given file name patterns.
	 */
	public static FileTextSearchScope newSearchScope(IWorkingSet[] workingSets, String[] fileNamePatterns,
			boolean includeDerived) {
		String description;
		Arrays.sort(workingSets, new WorkingSetComparator());
		if (workingSets.length == 0) {
			description = SearchMessages.FileTextSearchScope_ws_scope_empty;
		} else if (workingSets.length == 1) {
			String label = SearchMessages.FileTextSearchScope_ws_scope_single;
			description = Messages.format(label, workingSets[0].getLabel());
		} else if (workingSets.length == 2) {
			String label = SearchMessages.FileTextSearchScope_ws_scope_double;
			description = Messages.format(label, new String[] { workingSets[0].getLabel(), workingSets[1].getLabel() });
		} else {
			String label = SearchMessages.FileTextSearchScope_ws_scope_multiple;
			description = Messages.format(label, new String[] { workingSets[0].getLabel(), workingSets[1].getLabel() });
		}
		FileTextSearchScope scope = new FileTextSearchScope(description, convertToResources(workingSets), workingSets,
				fileNamePatterns, includeDerived);
		return scope;
	}

	/**
	 * @param fileNamePatterns
	 *            uhad
	 * @param includeDerived
	 *            jkshd
	 * @return kjsd
	 * @since 3.16
	 */
	public static FileTextSearchScope newWorkspaceScope(String[] fileNamePatterns, boolean includeDerived) {
		IResource[] searchScope = collectAllJarAndClassFilesInWorkspaceAndClasspath();
		return new FileTextSearchScope(SearchMessages.WorkspaceScope, searchScope, null, fileNamePatterns,
				includeDerived);
	}

	// Solution with bytes in Map (not working if trying to turn into Files)
	// private static IResource[]
	// collectAllJarAndClassFilesInWorkspaceAndClasspath() {
	// List<IResource> resources = new ArrayList<>();
	// try {
	// IJavaProject[] javaProjects =
	// JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
	// for (IJavaProject javaProject : javaProjects) {
	// IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
	// for (IClasspathEntry entry : classpathEntries) {
	// if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
	// IClasspathEntry[] containerEntries = JavaCore
	// .getClasspathContainer(entry.getPath(),
	// javaProject).getClasspathEntries();
	// for (IClasspathEntry containerEntry : containerEntries) {
	// if (containerEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
	// IPath jarPath = containerEntry.getPath();
	// File jarFile = jarPath.toFile();
	// if (jarFile.exists()) {
	// List<File> extractedFiles = new ArrayList<>();
	// Map<String, byte[]> extractedData = readAllFilesFromJar(jarFile);
	// Set<Map.Entry<String, byte[]>> set = extractedData.entrySet();
	// for (Map.Entry<String, byte[]> setEntry : set) {
	// byte[] content = setEntry.getValue();
	// File outputFile = new File(setEntry.getKey());
	// try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
	// outputStream.write(content);
	// extractedFiles.add(outputFile);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	// } catch (Exception e) {
	// // Handle
	// }
	// return resources.toArray(new IResource[resources.size()]);
	// }

	// Solution with bytes in Map (not working if trying to turn into Files)
	// private static Map<String, byte[]> readAllFilesFromJar(File jarFile) {
	// Map<String, byte[]> extractedFiles = new HashMap<>();
	//
	// try (FileInputStream fileInputStream = new FileInputStream(jarFile);
	// ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
	//
	// byte[] buffer = new byte[1024];
	// int bytesRead;
	//
	// ZipEntry entry;
	// while ((entry = zipInputStream.getNextEntry()) != null) {
	// if (!entry.isDirectory()) {
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// while ((bytesRead = zipInputStream.read(buffer)) != -1) {
	// baos.write(buffer, 0, bytesRead);
	// }
	// extractedFiles.put(entry.getName(), baos.toByteArray());
	// }
	// }
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// return extractedFiles;
	// }

	private static IResource[] collectAllJarAndClassFilesInWorkspaceAndClasspath() {
		List<IResource> resources = new ArrayList<>();
		try {
			IJavaProject[] javaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			for (IJavaProject javaProject : javaProjects) {
				IProject project = javaProject.getProject();
				IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
				for (IClasspathEntry entry : classpathEntries) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						IClasspathEntry[] containerEntries = JavaCore
								.getClasspathContainer(entry.getPath(), javaProject).getClasspathEntries();
						for (IClasspathEntry containerEntry : containerEntries) {
							if (containerEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
								List<IPath> paths = new ArrayList<>();
								IPath path = containerEntry.getPath();
								JarFile jar = new JarFile(path.toFile());
								Enumeration<JarEntry> entries = jar.entries();
								while (entries.hasMoreElements()) {
									JarEntry jarEntry = entries.nextElement();
									IPath entryPath = new Path(jarEntry.getName());
									paths.add(entryPath);
								}
								for (IPath jarPath : paths) {
									IFile file = project.getFile(jarPath);
									resources.add(file);
								}
								IFile file = project.getFile(path);
								resources.add(file);
							}
						}
					} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						List<IPath> paths = new ArrayList<>();
						IPath path = entry.getPath();
						IFile jarFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
						JarFile jar = new JarFile(jarFile.getLocation().toFile());
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							JarEntry jarEntry = entries.nextElement();
							IPath entryPath = new Path(jarEntry.getName());
							paths.add(entryPath);
						}
						for (IPath jarPath : paths) {
							IFile file = project.getFile(jarPath);
							resources.add(file);
						}
						IFile file = project.getFile(path);
						resources.add(file);
					} else if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath sourcePath = entry.getPath();
						IFile file = project.getFile(sourcePath);
						if (file instanceof IContainer) {
							IContainer sourceContainer = (IContainer) file;
							processSourceContainer(sourceContainer, resources);
						} else {
							resources.add(file);
						}
					}
				}
			}
		} catch (Exception e) {
			// Handle
		}
		resources.add(ResourcesPlugin.getWorkspace().getRoot());
		return resources.toArray(new IResource[resources.size()]);
	}

	private static void processSourceContainer(IContainer sourceContainer, List<IResource> resources)
			throws CoreException {
		IResource[] sourceMembers = sourceContainer.members();

		for (IResource member : sourceMembers) {
			if (member instanceof IFile javaSourceFile) {
				resources.add(javaSourceFile);
			} else if (member instanceof IContainer nestedContainer) {
				processSourceContainer(nestedContainer, resources);
			}
		}
	}

	private final String fDescription;
	private final IResource[] fRootElements;
	private final String[] fFileNamePatterns;
	private final ThreadLocal<Matcher> fPositiveFileNameMatcher;
	private final ThreadLocal<Matcher> fNegativeFileNameMatcher;

	private boolean fVisitDerived;
	private IWorkingSet[] fWorkingSets;

	private FileTextSearchScope(String description, IResource[] resources, IWorkingSet[] workingSets,
			String[] fileNamePatterns, boolean visitDerived) {
		fDescription = description;
		fRootElements = resources;
		fFileNamePatterns = fileNamePatterns;
		fVisitDerived = visitDerived;
		fWorkingSets = workingSets;
		fPositiveFileNameMatcher = ThreadLocal.withInitial(() -> createMatcher(fileNamePatterns, false));
		fNegativeFileNameMatcher = ThreadLocal.withInitial(() -> createMatcher(fileNamePatterns, true));
	}

	/**
	 * Returns the description of the scope
	 *
	 * @return the description of the scope
	 */
	public String getDescription() {
		return fDescription;
	}

	/**
	 * Returns the file name pattern configured for this scope or
	 * <code>null</code> to match all file names.
	 *
	 * @return the file name pattern starings
	 */
	public String[] getFileNamePatterns() {
		return fFileNamePatterns;
	}

	/**
	 * Returns the working-sets that were used to configure this scope or
	 * <code>null</code> if the scope was not created off working sets.
	 *
	 * @return the working-sets the scope is based on.
	 */
	public IWorkingSet[] getWorkingSets() {
		return fWorkingSets;
	}

	/**
	 * Returns the content types configured for this scope or <code>null</code>
	 * to match all content types.
	 *
	 * @return the file name pattern starings
	 */
	public IContentType[] getContentTypes() {
		return null; // to be implemented in the future
	}

	/**
	 * Returns a description describing the file name patterns and content
	 * types.
	 *
	 * @return the description of the scope
	 */
	public String getFilterDescription() {
		String[] ext = fFileNamePatterns;
		if (ext == null) {
			return BasicElementLabels.getFilePattern("*"); //$NON-NLS-1$
		}
		Arrays.sort(ext);
		return BasicElementLabels.getFilePattern(String.join(", ", ext)); //$NON-NLS-1$
	}

	/**
	 * Returns whether derived resources are included in this search scope.
	 *
	 * @return whether derived resources are included in this search scope.
	 */
	public boolean includeDerived() {
		return fVisitDerived;
	}

	@Override
	public IResource[] getRoots() {
		return fRootElements;
	}

	@Override
	public boolean contains(IResourceProxy proxy) {
		if (!fVisitDerived && proxy.isDerived()) {
			return false; // all resources in a derived folder are considered to
							// be derived, see bug 103576
		}

		if (proxy.getType() == IResource.FILE) {
			return matchesFileName(proxy.getName());
		}
		return true;
	}

	private boolean matchesFileName(String fileName) {
		Matcher positiveFileNameMatcher = fPositiveFileNameMatcher.get();
		if (positiveFileNameMatcher != null && !positiveFileNameMatcher.reset(fileName).matches()) {
			return false;
		}
		Matcher negativeFileNameMatcher = fNegativeFileNameMatcher.get();
		if (negativeFileNameMatcher != null && negativeFileNameMatcher.reset(fileName).matches()) {
			return false;
		}
		return true;
	}

	private Matcher createMatcher(String[] fileNamePatterns, boolean negativeMatcher) {
		if (fileNamePatterns == null || fileNamePatterns.length == 0) {
			return null;
		}
		ArrayList<String> patterns = new ArrayList<>();
		for (int i = 0; i < fileNamePatterns.length; i++) {
			String pattern = fFileNamePatterns[i];
			if (negativeMatcher == pattern.startsWith(FileTypeEditor.FILE_PATTERN_NEGATOR)) {
				if (negativeMatcher) {
					pattern = pattern.substring(FileTypeEditor.FILE_PATTERN_NEGATOR.length()).trim();
				}
				if (!pattern.isEmpty()) {
					patterns.add(pattern);
				}
			}
		}
		if (!patterns.isEmpty()) {
			String[] patternArray = patterns.toArray(new String[patterns.size()]);
			Pattern pattern = PatternConstructor.createPattern(patternArray, IS_CASE_SENSITIVE_FILESYSTEM);
			return pattern.matcher(""); //$NON-NLS-1$
		}
		return null;
	}

	private static IResource[] removeRedundantEntries(IResource[] elements, boolean includeDerived) {
		ArrayList<IResource> res = new ArrayList<>();
		for (IResource curr : elements) {
			addToList(res, curr, includeDerived);
		}
		return res.toArray(new IResource[res.size()]);
	}

	// private static IResource[] convertToResources(IWorkingSet[] workingSets,
	// boolean includeDerived) {
	// ArrayList<IResource> res= new ArrayList<>();
	// for (IWorkingSet workingSet : workingSets) {
	// if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
	// return new IResource[] { ResourcesPlugin.getWorkspace().getRoot() };
	// }
	// IAdaptable[] elements= workingSet.getElements();
	// for (IAdaptable element : elements) {
	// IResource curr= element.getAdapter(IResource.class);
	// if (curr != null) {
	// addToList(res, curr, includeDerived);
	// }
	// }
	// }
	// return res.toArray(new IResource[res.size()]);
	// }

	private static IResource[] convertToResources(IWorkingSet[] workingSets) {
		String searchString = "Test"; //$NON-NLS-1$
		ArrayList<IResource> res = new ArrayList<>();
		for (IWorkingSet workingSet : workingSets) {
			if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
				return new IResource[] { ResourcesPlugin.getWorkspace().getRoot() };
			}
			IAdaptable[] elements = workingSet.getElements();
			for (IAdaptable element : elements) {
				IJavaProject javaProject = JavaCore.create(element.getAdapter(IProject.class));
				if (javaProject != null && javaProject.exists()) {
					collectClassFilesFromJavaProject(javaProject, res, searchString);
				}
			}
		}
		return res.toArray(new IResource[res.size()]);
	}

	private static void collectClassFilesFromJavaProject(IJavaProject javaProject, ArrayList<IResource> res,
			String searchString) {
		try {
			IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpathEntries) {
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
				if (resource != null) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						collectClassFilesFromSourceFolder(javaProject, entry, res, searchString);
					} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						if (resource.getType() == IResource.FILE && resource.getName().toLowerCase().endsWith(".jar")) { //$NON-NLS-1$
							collectClassFilesFromClasspathJar(resource, res, searchString);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// Handle the exception
		}
	}

	private static void collectClassFilesFromSourceFolder(IJavaProject javaProject, IClasspathEntry entry,
			ArrayList<IResource> res, String searchString) {
		try {
			IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(entry);
			for (IPackageFragmentRoot root : roots) {
				IJavaElement[] children = root.getChildren();
				for (IJavaElement child : children) {
					if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment packageFragment = (IPackageFragment) child;
						ICompilationUnit[] units = packageFragment.getCompilationUnits();
						for (ICompilationUnit unit : units) {
							IResource unitResource = unit.getResource();
							if (unitResource != null) {
								if (searchStringInCompilationUnit(unit, searchString)) {
									res.add(unitResource);
								}
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// Handle the exception
		}
	}

	private static void collectClassFilesFromClasspathJar(IResource jarResource, ArrayList<IResource> res,
			String searchString) {
		try (ZipFile zipFile = new ZipFile(jarResource.getLocation().toOSString())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class")) { //$NON-NLS-1$
					try (InputStream inputStream = zipFile.getInputStream(entry)) {
						byte[] classBytes = inputStream.readAllBytes();
						String classContent = new String(classBytes);
						if (classContent.contains(searchString)) {
							res.add(jarResource);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean searchStringInCompilationUnit(ICompilationUnit compilationUnit, String searchString) {
		try {
			String source = compilationUnit.getSource();
			return source.contains(searchString);
		} catch (JavaModelException e) {
			// Handle the exception
		}
		return false;
	}

	private static void addToList(ArrayList<IResource> res, IResource curr, boolean includeDerived) {
		if (!includeDerived && curr.isDerived(IResource.CHECK_ANCESTORS)) {
			return;
		}
		IPath currPath = curr.getFullPath();
		for (int k = res.size() - 1; k >= 0; k--) {
			IResource other = res.get(k);
			IPath otherPath = other.getFullPath();
			if (otherPath.isPrefixOf(currPath)) {
				return;
			}
			if (currPath.isPrefixOf(otherPath)) {
				res.remove(k);
			}
		}
		res.add(curr);
	}
}
