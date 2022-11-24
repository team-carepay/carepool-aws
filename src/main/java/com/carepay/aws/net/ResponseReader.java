package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;

@FunctionalInterface
public interface ResponseReader<T> {
    T read(HttpURLConnection uc) throws IOException;
}
