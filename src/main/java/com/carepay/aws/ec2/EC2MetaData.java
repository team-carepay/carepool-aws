package com.carepay.aws.ec2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import com.carepay.aws.util.URLOpener;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides access to the EC2 metadata. This can be used to retrieve instance-id and region
 * information.
 */
public class EC2MetaData {
    /**
     * EC2 metadata URL
     */
    public static final URL META_DATA_URL = createURL("http://169.254.169.254/latest/dynamic/instance-identity/document");

    private final URLOpener opener;

    public EC2MetaData() {
        this(URLOpener.DEFAULT);
    }

    public EC2MetaData(URLOpener opener) {
        this.opener = opener;
    }

    static URL createURL(final String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String queryMetaDataAsString(final URL url) {
        try {
            final HttpURLConnection uc = opener.open(url);
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            final int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream();
                 final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (responseCode >= 400) {
                    return null;
                }
                final byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) {
                    baos.write(buf, 0, n);
                }
                return baos.toString();
            } finally {
                uc.disconnect();
            }
        } catch (IOException e) { // NOSONAR
            return null;
        }
    }

    public String getInstanceId() {
        return queryMetaData(META_DATA_URL).get("instanceId");
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> queryMetaData(final URL url) {
        try {
            final HttpURLConnection uc = opener.open(url);
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            try (final InputStream is = uc.getInputStream();
                 final InputStreamReader reader = new InputStreamReader(is, UTF_8)) {
                return (Map<String, String>) Jsoner.deserialize(reader);
            } finally {
                uc.disconnect();
            }
        } catch (IOException | JsonException e) { // NOSONAR
            return Collections.emptyMap();
        }
    }
}
