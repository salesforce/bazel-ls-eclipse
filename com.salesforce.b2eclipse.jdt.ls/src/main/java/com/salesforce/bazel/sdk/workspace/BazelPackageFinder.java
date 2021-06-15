package com.salesforce.bazel.sdk.workspace;

import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.salesforce.bazel.sdk.logging.LogHelper;
import com.salesforce.bazel.sdk.model.BazelBuildFileHelper;
import com.salesforce.bazel.sdk.util.BazelConstants;
import com.salesforce.bazel.sdk.util.BazelPathHelper;
import com.salesforce.bazel.sdk.util.WorkProgressMonitor;

public class BazelPackageFinder {
    private LogHelper logger;

    public BazelPackageFinder() {
        logger = LogHelper.log(this.getClass());
    }

    // TODO our workspace scanner is looking for Java packages, but uses primitive techniques. switch to use the aspect
    // approach here, like we do with the classpath computation.

    private void findBuildFileLocations(File dir, WorkProgressMonitor monitor, Set<File> buildFileLocations, int depth) throws IOException {
        if (!dir.isDirectory()) {
            return;
        }

        // collect all BUILD files
        List<Path> buildFiles = new ArrayList<>(1000);

        Path start = dir.toPath();
        Files.walkFileTree(start, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(start.relativize(dir).toString().startsWith("bazel-"))
                    // this is a Bazel internal directory at the root of the project dir, ignore
                    return FileVisitResult.SKIP_SUBTREE;

                if(dir.getFileName().toString().equals("target"))
                    // skip Maven target directories
                    return FileVisitResult.SKIP_SUBTREE;

                if(dir.getFileName().toString().equals(".bazel"))
                    // skip Core .bazel directory
                    return FileVisitResult.SKIP_SUBTREE;

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(isBuildFile(file)) {
                    buildFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

        });

        // scan all build files
        buildFiles.parallelStream().forEach(file -> {
            // great, this dir is a Bazel package (but this may be a non-Java package)
            // scan the BUILD file looking for java rules, only add if this is a java project
            if (BazelBuildFileHelper.hasJavaRules(file.toFile())) {
                buildFileLocations.add(BazelPathHelper.getCanonicalFileSafely(file.getParent().toFile()));
            }
        });

    }

    private static boolean isBuildFile(Path candidate) {
        return BazelConstants.BUILD_FILE_NAMES.contains(candidate.getFileName().toString());
    }

    public Set<File> findBuildFileLocations(File rootDirectoryFile) throws IOException {
        Set<File> files = ConcurrentHashMap.newKeySet();
        findBuildFileLocations(rootDirectoryFile, null, files, 0);
        return files;
    }
}
