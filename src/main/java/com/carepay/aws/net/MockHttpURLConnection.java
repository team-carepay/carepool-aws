package com.carepay.aws.net;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MockHttpURLConnection extends HttpURLConnection {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public MockHttpURLConnection(URL u) {
        super(u);
    }

    @Override
    public OutputStream getOutputStream() {
        return output;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() {
    }

    public byte[] getBytes() {
        return output.toByteArray();
    }
}
