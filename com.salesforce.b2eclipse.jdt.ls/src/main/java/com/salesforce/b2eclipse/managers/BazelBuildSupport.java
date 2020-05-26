package com.salesforce.b2eclipse.managers;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;

@SuppressWarnings("restriction")
public class BazelBuildSupport implements IBuildSupport{

	private static final String BUILD_FILE_PATTERN = "**/BUILD";
	private static final String WORKSPACE_FILE_PATTERN = "**/WORKSPACE";
	
    @Override
    public boolean applies(IProject project) {
        // TODO Auto-generated method stub
        return false;
    }

	@Override
	public boolean isBuildFile(IResource resource) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public List<String> getBasicWatchers() {
        return Arrays.asList(BUILD_FILE_PATTERN, WORKSPACE_FILE_PATTERN);
    }

}
