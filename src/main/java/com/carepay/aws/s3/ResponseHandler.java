package com.carepay.aws.s3;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * ResponseHandler is an interface used to extract information from an ongoing HttpURLConnection
 */
@FunctionalInterface
public interface ResponseHandler {
    String extract(HttpURLConnection urlConnection) throws IOException;
}
