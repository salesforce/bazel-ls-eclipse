package com.salesforce.bazel.eclipse;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;

import com.salesforce.bazel.eclipse.abstractions.WorkProgressMonitor;
import com.salesforce.bazel.eclipse.config.BazelEclipseProjectFactory;
import com.salesforce.bazel.eclipse.importer.BazelProjectImportScanner;
import com.salesforce.bazel.eclipse.model.BazelPackageInfo;
import com.salesforce.bazel.eclipse.runtime.impl.EclipseWorkProgressMonitor;

public final class BazelProjectImporter extends AbstractProjectImporter {

	private static final String WORKSPACE_FILE_NAME = "WORKSPACE";

	@Override
	public boolean applies(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (!rootFolder.exists() || !rootFolder.isDirectory()) {
			return false;
		}
		File pomFile = new File(rootFolder, WORKSPACE_FILE_NAME);
		if (!pomFile.exists()) {
			return false;
		}
		return true;

	}

	@Override
	public void importToWorkspace(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		BazelProjectImportScanner scanner = new BazelProjectImportScanner();

		BazelPackageInfo workspaceRootPackage = scanner.getProjects(rootFolder);

		if (workspaceRootPackage == null) {
			throw new IllegalArgumentException();
        }
		List<BazelPackageInfo> bazelPackagesToImport =
				workspaceRootPackage.getChildPackageInfos().stream().collect(Collectors.toList());

		WorkProgressMonitor progressMonitor = new EclipseWorkProgressMonitor(null);

		BazelEclipseProjectFactory.importWorkspace(workspaceRootPackage, bazelPackagesToImport, progressMonitor, monitor);
	}

	@Override
	public void reset() {

	}

}
