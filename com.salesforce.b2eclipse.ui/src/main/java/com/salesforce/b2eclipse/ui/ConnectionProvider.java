package com.salesforce.b2eclipse.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class ConnectionProvider implements StreamConnectionProvider {
	
	private static final String B2E_DEBUG_VM_ARG = "b2e.debug";
	
	private final StreamConnectionProvider provider;
	
	public ConnectionProvider() {
		String debug = System.getProperty(B2E_DEBUG_VM_ARG);
		
		if (debug == null) {
			provider = new ProcessStreamConnectionProvider(
					Config.getInstance().getConnectionProviderProcessWorkingDir()
			);
		} else {
			provider = new SocketStreamConnectionProvider(
					Config.getInstance().getConnectionProviderSocketPort()
			);
		}
	}

	@Override
	public void start() throws IOException {
		provider.start();
	}

	@Override
	public InputStream getInputStream() {
		return provider.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return provider.getOutputStream();
	}

	@Override
	public InputStream getErrorStream() {
		return provider.getErrorStream();
	}

	@Override
	public void stop() {
		provider.stop();
	}

	@Override
	public String toString() {
		return provider.toString();
	}
	
	@Override
	public Object getInitializationOptions(URI rootUri){
		return Config.getInstance().getLanguageServerInitOptions();
	}
}
