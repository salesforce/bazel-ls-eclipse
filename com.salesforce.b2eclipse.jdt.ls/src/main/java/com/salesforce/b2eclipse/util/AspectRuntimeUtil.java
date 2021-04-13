package com.salesforce.b2eclipse.util;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.salesforce.b2eclipse.abstractions.BazelAspectLocation;
import com.salesforce.b2eclipse.model.AspectPackageInfo;

import org.apache.commons.lang3.StringUtils;

public final class AspectRuntimeUtil {
    private static final String ENV_ASPECT_VERSION = "aspectVersion"; //$NON-NLS-1$
    private static final String BEF_ASPECT = "bef"; //$NON-NLS-1$
    private static final String INTELLIJ_ASPECT_FILENAME_SUFFIX = ".intellij-info.txt"; //$NON-NLS-1$

    private AspectRuntimeUtil() {

    }

    public static boolean isBefAspectVersion() {
        String aspectVersion = System.getProperty(ENV_ASPECT_VERSION);
        boolean isBef = BEF_ASPECT.equalsIgnoreCase(StringUtils.trimToNull(aspectVersion));
        return isBef;
    }

    public static ImmutableMap<String, AspectPackageInfo> loadAspectFilePaths(List<String> files)
            throws IOException, InterruptedException {
        return isBefAspectVersion() ? //
                AspectPackageInfo.loadAspectFilePaths(files) : IntellijAspectPackageInfoLoader.loadAspectFiles(files);
    }

    public static ImmutableList<String> buildAspectsOptions(BazelAspectLocation aspectLocation) {
        return isBefAspectVersion() ? //
                buildBefAspect(aspectLocation) : buildIntellijAspect(aspectLocation);
    }

    public static Function<String, String> buildAspectFilter() {
        return isBefAspectVersion() ? //
                (t -> t.startsWith(">>>")
                        ? (t.endsWith(AspectPackageInfo.ASPECT_FILENAME_SUFFIX) ? t.replace(">>>", "") : "") : null)
                : //
                (t -> t.startsWith(">>>") ? (t.endsWith(INTELLIJ_ASPECT_FILENAME_SUFFIX) ? t.replace(">>>", "") : "") : null);
    }

    private static ImmutableList<String> buildIntellijAspect(BazelAspectLocation aspectLocation) {
        return ImmutableList.<String>builder().add("--nobuild_event_binary_file_path_conversion") //
                .add("--noexperimental_run_validations") //
                .add("--aspects=@intellij_aspect//:intellij_info_bundled.bzl%intellij_info_aspect") //
                .add("--override_repository=intellij_aspect=" + aspectLocation.getAspectDirectory()) //
                .add(
                    "--output_groups=intellij-info-generic,intellij-info-java-direct-deps,intellij-resolve-java-direct-deps") //
                .add("--experimental_show_artifacts")//
                .build();
    }

    private static ImmutableList<String> buildBefAspect(BazelAspectLocation aspectLocation) {
        return ImmutableList.<String>builder()
                .add("--override_repository=local_eclipse_aspect=" + aspectLocation.getAspectDirectory(),
                    "--aspects=@local_eclipse_aspect" + aspectLocation.getAspectLabel(), "-k",
                    "--output_groups=json-files,classpath-jars,-_,-defaults", "--experimental_show_artifacts")
                .build();
    }
}
