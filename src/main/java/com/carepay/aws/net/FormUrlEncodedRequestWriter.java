package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FormUrlEncodedRequestWriter implements RequestWriter {

    private final Map<String, String> parameters;

    public FormUrlEncodedRequestWriter(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void write(HttpURLConnection os) throws IOException {
        os.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        final byte[] bytes = FormUrlEncodedUtils.encode(parameters).getBytes(StandardCharsets.UTF_8);
        os.setFixedLengthStreamingMode(bytes.length);
        os.getOutputStream().write(bytes);
    }
}
