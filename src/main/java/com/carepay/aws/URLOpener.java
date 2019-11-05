package com.carepay.aws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public interface URLOpener {
    HttpURLConnection open (URL url) throws IOException;
    URLOpener DEFAULT = url -> (HttpURLConnection)url.openConnection();
}
