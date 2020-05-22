package com.salesforce.b2eclipse.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.command.BazelCommandManager;
import com.salesforce.b2eclipse.command.BazelWorkspaceCommandRunner;
import com.salesforce.b2eclipse.runtime.impl.EclipseWorkProgressMonitor;

final class ProjectFileScanner {
    
    private ProjectFileScanner() {
    }

    private static final String BAZELTARGETSFILENAME = ".bazeltargets";

    public static List<String> getConfiguredTargets(File rootDirectoryFile) {
        File projectFile = new File(rootDirectoryFile, BAZELTARGETSFILENAME);

//        List<File> targets = Lists.newArrayList();
        List<String> targets = Lists.newArrayList();

        try (FileReader fr = new FileReader(projectFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                //  TODO: ignore #comments
//                targets.add(new File(rootDirectoryFile, line.replaceAll("^/+", "")));
                targets.add(line);
            }

        BazelCommandManager bazelCommandManager = BazelJdtPlugin.getBazelCommandManager();
        BazelWorkspaceCommandRunner bazelWorkspaceCmdRunner =
                bazelCommandManager.getWorkspaceCommandRunner(BazelJdtPlugin.getBazelWorkspaceRootDirectory());
        
        return bazelWorkspaceCmdRunner.getPackagesForTargets(targets, new EclipseWorkProgressMonitor(null));
            
        } catch (IOException e) {
            BazelJdtPlugin.logError("ERROR reading " + BAZELTARGETSFILENAME + " file:" + e.getMessage());
            return null;
        }
    }

}
