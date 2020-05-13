package com.salesforce.b2eclipse.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

final class ProjectFileScanner {
    
    private ProjectFileScanner() {
    }

    private static final String BAZELPROJECTFILENAME = ".bazelproject";

    public static List<File> getConfiguredTargets(File rootDirectoryFile) {
        File projectFile = new File(rootDirectoryFile, BAZELPROJECTFILENAME);

        List<File> targets = Lists.newArrayList();

        try (FileReader fr = new FileReader(projectFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                targets.add(new File(rootDirectoryFile, line.replaceAll("^/+", "")));
            }

        } catch (IOException e) {
            return null;
        }

        return targets;
    }

}
