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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class SocketStreamConnectionProvider implements StreamConnectionProvider {

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
