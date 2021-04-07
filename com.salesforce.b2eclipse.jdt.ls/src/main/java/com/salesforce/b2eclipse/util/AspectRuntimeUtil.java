package com.salesforce.b2eclipse.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.salesforce.b2eclipse.BazelJdtPlugin;
import com.salesforce.b2eclipse.model.AspectPackageInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public final class AspectRuntimeUtil {
    private AspectRuntimeUtil() {
    }

    public static boolean isBefAspectVersion() {
        String aspectVersion = System.getProperty("aspectVersion");
        boolean isBef = "bef".equalsIgnoreCase(StringUtils.trimToNull(aspectVersion));
        return isBef;
    }

    public static List<String> filterIntellijOutput(List<String> outputLines, Collection<String> targets) {
        final ArrayList<String> modules = new ArrayList<String>();
        final boolean isWindows = SystemUtils.IS_OS_WINDOWS;
        final Pattern pattern = Pattern.compile(".*\\.intellij-info\\.txt");
        BazelJdtPlugin.logInfo("##### Targets : " + targets.toString());

        outputLines.stream().filter((line) -> {
            Matcher matcher = pattern.matcher(line);
            return matcher.matches();
        }).forEach((String intellijOutputModule) -> {
            String jsonFile = intellijOutputModule.replaceAll("\\.intellij-info\\.txt", "")
                    + AspectPackageInfo.ASPECT_FILENAME_SUFFIX;

            try {
                int exitCode = isWindows ? generateWindowsJson(intellijOutputModule, jsonFile)
                        : generateLinuxJson(intellijOutputModule, jsonFile);

                if (0 == exitCode) {
                    modules.add(jsonFile);
                }
            } catch (IOException exc) {
                BazelJdtPlugin.logException(exc);
            } catch (InterruptedException exc) {
                BazelJdtPlugin.logException(exc);
            }
        });

        return modules;
    }

    private static int generateWindowsJson(String intellijOutputModule, String jsonFile)
            throws InterruptedException, IOException {
        String fromFile = intellijOutputModule.replaceAll("/", "\\\\");
        String toFile = jsonFile.replaceAll("/", "\\\\");
        URL fileUrl = BazelJdtPlugin.findResource("/resources/jq/runjq.bat");
        String cmd = fileUrl.getPath() + " " + fromFile + " " + toFile;
        Process process = Runtime.getRuntime().exec(cmd);
        int exitCode = process.waitFor();
        return exitCode;
    }

    private static int generateLinuxJson(String intellijOutputModule, String jsonFile)
            throws InterruptedException, IOException {
        URL fileUrl = BazelJdtPlugin.findResource("/resources/jq/runjq.sh");
        String cmd = "bash " + fileUrl.getPath() + " " + intellijOutputModule + " " + jsonFile;
        Process process = Runtime.getRuntime().exec(cmd);
        int exitCode = process.waitFor();
        return exitCode;
    }
}
