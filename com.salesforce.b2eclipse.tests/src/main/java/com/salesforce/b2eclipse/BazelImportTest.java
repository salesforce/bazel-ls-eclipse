package com.salesforce.b2eclipse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;

import com.salesforce.b2eclipse.config.BazelEclipseProjectFactory;
import com.salesforce.b2eclipse.importer.BazelProjectImportScanner;
import com.salesforce.b2eclipse.model.BazelPackageInfo;
import com.salesforce.b2eclipse.runtime.impl.EclipseWorkProgressMonitor;

 

public class BazelImportTest {
	
	private IWorkspaceRoot workspaceRoot;
	
	static {
		BazelEclipseProjectFactory.IMPORT_BAZEL_SRC_PATH = "/java/src";
	}
	
	public BazelImportTest() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
        this.workspaceRoot = workspace.getRoot();        
	}
    
    @Before
    public void setup() {
        BazelProjectImportScanner scanner = new BazelProjectImportScanner();
        BazelPackageInfo workspaceRootPackage = scanner.getProjects("projects/bazel-ls-demo-project");
        
        List<BazelPackageInfo> bazelPackagesToImport = 
                workspaceRootPackage.getChildPackageInfos().stream().collect(Collectors.toList());
        
        BazelEclipseProjectFactory.importWorkspace(
        		workspaceRootPackage,
        		bazelPackagesToImport,
        		new EclipseWorkProgressMonitor(),
        		new NullProgressMonitor()
        );
    }
    
    @Test
    public void testImport() throws CoreException {
    	IProject module1Proj = workspaceRoot.getProject("module1");
    	IProject module2Proj = workspaceRoot.getProject("module2");
        IProject module3Proj = workspaceRoot.getProject("module3");
        
    	IProject[] referencedProjects = module1Proj.getReferencedProjects();
    	
    	assertEquals(2, referencedProjects.length);
    	
    	assertNotNull(
    			"Didn't find module2 in the referenced projects list", 
    			Arrays.stream(referencedProjects).anyMatch(proj -> proj.equals(module2Proj))
    	);
    	
    	assertNotNull(
    			"Didn't find module3 in the referenced projects list",
    			Arrays.stream(referencedProjects).anyMatch(proj -> proj.equals(module3Proj))
    	);    	
    }
}