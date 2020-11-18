package com.carepay.aws.s3;

import java.io.IOException;
import java.net.HttpURLConnection;

@FunctionalInterface
public interface ResponseHandler {
    String extract(HttpURLConnection urlConnection) throws IOException;
}
