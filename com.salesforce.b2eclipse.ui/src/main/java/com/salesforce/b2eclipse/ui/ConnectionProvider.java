package com.salesforce.b2eclipse.ui;

import java.io.InputStream;

public class ConnectionProvider extends SocketStreamConnectionProvider {
	
	private static final int PORT = 5036; //TODO: hardcoded value

	public ConnectionProvider() {
		super(PORT); 	
	}

	@Override
	public String toString() {
		return "Java Language Server Connection Provider" + super.toString();
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}

}