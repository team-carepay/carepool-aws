package com.carepay.aws.ec2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
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
public class ResourceFetcher {

    @SuppressWarnings("unchecked")
    static final Converter<Map<String, String>> JSON = reader -> {
        try {
            return (Map<String, String>) Jsoner.deserialize(reader);
        } catch (JsonException e) {
            throw new IOException(e);
        }
    };
    static final Converter<String> FIRST_LINE = reader -> {
        try (final BufferedReader br = new BufferedReader(reader)) {
            return br.readLine();
        }
    };
    private final URLOpener opener;

    public ResourceFetcher() {
        this(new URLOpener.Default());
    }

    public ResourceFetcher(URLOpener opener) {
        this.opener = opener;
    }

    public <T> T query(final URL url, final Converter<T> converter, final Map<String, String> headers) throws IOException {
        final HttpURLConnection uc = opener.open(url);
        uc.setConnectTimeout(1000);
        uc.setReadTimeout(1000);
        if (headers != null) {
            headers.forEach(uc::setRequestProperty);
        }
        try (final InputStream is = uc.getInputStream();
             final InputStreamReader reader = is != null ? new InputStreamReader(is, UTF_8) : null) {
            return reader != null ? converter.convert(reader) : null;
        } finally {
            uc.disconnect();
        }
    }

    public Map<String, String> queryJson(final URL url) {
        return queryJson(url, null);
    }

    public Map<String, String> queryJson(final URL url, final Map<String, String> headers) {
        try {
            return query(url, JSON, headers);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public String queryFirstLine(final URL url) {
        try {
            return query(url, FIRST_LINE, null);
        } catch (IOException e) {
            return null;
        }
    }

    @FunctionalInterface
    interface Converter<T> {
        T convert(Reader reader) throws IOException;
    }
}
