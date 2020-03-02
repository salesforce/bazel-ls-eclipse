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
			return Files.walk(Paths.get(workingDir))
				.filter(Files::isRegularFile)
				.filter(f -> f.getFileName().toString().contains("org.eclipse.equinox.launcher_"))
				.findFirst()
				.orElseThrow(() -> {
					B2EPlugin.logError("Can't find equinox launcher executable", null);
				    return new RuntimeException();
				})
				.getFileName()
				.toString();
		} catch (Exception e) {
			B2EPlugin.logError(e);
			throw new RuntimeException();
		}
	}
}

