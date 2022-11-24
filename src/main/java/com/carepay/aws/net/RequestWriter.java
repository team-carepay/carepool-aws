package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;

@FunctionalInterface
public interface RequestWriter {
    void write(HttpURLConnection os) throws IOException;
}
