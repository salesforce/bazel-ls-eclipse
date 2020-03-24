/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.salesforce.b2eclipse.ui;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public class ProcessStreamConnectionProvider extends org.eclipse.lsp4e.server.ProcessStreamConnectionProvider {

    public ProcessStreamConnectionProvider(String workingDir) {
        String equinoxLauncherName = findEquinoxLauncherName(workingDir);

        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044");
        commands.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
        commands.add("-Dosgi.bundles.defaultStartLevel=4");
        commands.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
        commands.add("-Dlog.protocol=true");
        commands.add("-Dlog.level=ALL");
        commands.add("-noverify");
        commands.add("-Xmx1G");
        commands.add("-jar");
        //change org.eclipse.equinox.launcher version for current
        commands.add("./plugins/" + equinoxLauncherName);
        commands.add("-configuration");
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
            commands.add("./config_win");
        }
        if (Platform.getOS().equals(Platform.OS_LINUX)) {
            commands.add("./config_linux");
        }
        if (Platform.getOS().equals(Platform.OS_MACOSX)) {
            commands.add("./config_mac");
        }
        commands.add("-data");
        commands.add("./data");

        commands.add("--add-modules=ALL-SYSTEM");
        commands.add("--add-opens");
        commands.add("java.base/java.util=ALL-UNNAMED");
        commands.add("--add-opens");
        commands.add("java.base/java.lang=ALL-UNNAMED");

        setCommands(commands);
        setWorkingDirectory(new Path(workingDir).toOSString());
    }

    private String findEquinoxLauncherName(String workingDir) {
        try {
            return Files.walk(Paths.get(workingDir)).filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().contains("org.eclipse.equinox.launcher_")).findFirst()
                    .orElseThrow(() -> {
                        B2EPlugin.logError("Can't find equinox launcher executable", null);
                        return new RuntimeException();
                    }).getFileName().toString();
        } catch (Exception e) {
            B2EPlugin.logError(e);
            throw new RuntimeException();
        }
    }
}
