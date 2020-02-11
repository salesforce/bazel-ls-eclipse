package com.salesforce.b2eclipse.ui;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

@SuppressWarnings("restriction")
public abstract class SocketStreamConnectionProvider implements StreamConnectionProvider{

	private int port;
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	
	public SocketStreamConnectionProvider(int port) {
		this.port = port;
	}
	
	@Override
	public InputStream getInputStream() {
		return new FilterInputStream(inputStream) {
			@Override
			public int read() throws IOException {
				int res = super.read();
				System.err.print((char) res);
				return res;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int bytes = super.read(b, off, len);
				byte[] payload = new byte[bytes];
				System.arraycopy(b, off, payload, 0, bytes);
				System.err.print(new String(payload));
				return bytes;
			}

			@Override
			public int read(byte[] b) throws IOException {
				int bytes = super.read(b);
				byte[] payload = new byte[bytes];
				System.arraycopy(b, 0, payload, 0, bytes);
				System.err.print(new String(payload));
				return bytes;
			}
		};
	}

	@Override
	public OutputStream getOutputStream() {
		return new FilterOutputStream(outputStream) {
			@Override
			public void write(int b) throws IOException {
				System.err.print((char) b);
				super.write(b);
			}

			@Override
			public void write(byte[] b) throws IOException {
				System.err.print(new String(b));
				super.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				byte[] actual = new byte[len];
				System.arraycopy(b, off, actual, 0, len);
				System.err.print(new String(actual));
				super.write(b, off, len);
			}
		};
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
				LanguageServerPlugin.logError("Stop ", e);
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
}
