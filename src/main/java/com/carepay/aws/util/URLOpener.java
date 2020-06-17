package com.carepay.aws.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This interface allows us to open HTTPS connections during unit-tests. The default implementation
 * simply opens a real HTTPS connection
 */
public interface URLOpener {
    HttpURLConnection open(URL url) throws IOException;

    URLOpener DEFAULT = url -> (HttpURLConnection) url.openConnection();

    static URL create(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
