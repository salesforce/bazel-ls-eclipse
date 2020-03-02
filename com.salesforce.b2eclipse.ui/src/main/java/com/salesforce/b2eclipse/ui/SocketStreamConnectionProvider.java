package com.salesforce.b2eclipse.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class SocketStreamConnectionProvider implements StreamConnectionProvider{

	private int port;
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	
	public SocketStreamConnectionProvider(int port) {
		this.port = port;
	}

	@Override
	public void start() throws IOException {
		socket = new Socket("localhost", port);

		if (socket == null) {
			throw new IOException("Unable to open socket: " + toString()); //$NON-NLS-1$
		}

		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();	
	}

	@Override
	public void stop() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				B2EPlugin.logError("Stop ", e);
			}
		}	
	}
	
	@Override
	public int hashCode() {
		int result = super.hashCode();
		return result ^ Objects.hashCode(this.port);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SocketStreamConnectionProvider)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		SocketStreamConnectionProvider other = (SocketStreamConnectionProvider) obj;
		return Objects.equals(this.socket, other.socket);
	}

	@Override
	public String toString() {
		return "SocketStreamConnectionProvider [socket=" + socket + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}
	
	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}
}
