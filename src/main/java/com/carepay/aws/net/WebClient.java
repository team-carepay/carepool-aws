package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Provides access to the EC2 metadata. This can be used to retrieve instance-id and region
 * information.
 */
public class WebClient {

    /*
        @SuppressWarnings("unchecked")
        static final ResponseReader<Map<String, String>> JSON = reader -> {
            try {
                return (Map<String, String>) Jsoner.deserialize(reader);
            } catch (JsonException e) {
                throw new IOException(e);
            }
        };
        static final ResponseReader<String> FIRST_LINE = reader -> {
            try (final BufferedReader br = new BufferedReader(reader)) {
                return br.readLine();
            }
        };
    */
    protected final URLOpener opener;

    public WebClient() {
        this(new URLOpener.Default());
    }

    public WebClient(URLOpener opener) {
        this.opener = opener;
    }

/*
    public <T> T get(final URL url, final ResponseReader<T> responseConverter, final Map<String, String> headers) throws IOException {
        return execute("GET", url, null, responseConverter, headers);
    }

    public <T> T post(final URL url, final RequestWriter requestWriter, final ResponseReader<T> responseConverter) throws IOException {
        return execute("POST", url, requestWriter, responseConverter, new HashMap<>());
    }

    public <T> T post(final URL url, final RequestWriter requestWriter, final ResponseReader<T> responseConverter, final Map<String, String> headers) throws IOException {
        return execute("POST", url, requestWriter, responseConverter, headers);
    }
*/

    public <T> T execute(final String method, final URL url, final RequestWriter requestWriter, final ResponseReader<T> responseConverter, final Map<String, String> headers) throws IOException {
        final HttpURLConnection uc = opener.open(url);
        try {
            uc.setDoOutput(requestWriter != null);
            uc.setRequestMethod(method);
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            if (headers != null) {
                headers.forEach(uc::setRequestProperty);
            }
            if (requestWriter != null) {
                requestWriter.write(uc);
            }
            assertResponseCode(uc);
            return responseConverter != null ? responseConverter.read(uc) : null;
        } finally {
            uc.disconnect();
        }
    }

/*
    public Map<String, String> queryJson(final URL url) {
        return queryJson(url, null);
    }

    public Map<String, String> queryJson(final URL url, final Map<String, String> headers) {
        try {
            return get(url, JSON, headers);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public String queryFirstLine(final URL url) {
        try {
            return get(url, FIRST_LINE, null);
        } catch (IOException e) {
            return null;
        }
    }
*/

    protected void assertResponseCode(final HttpURLConnection uc) throws IOException {

        if (hasFailed(uc)) {
            handleFailedResponse(uc);
        }
    }

    protected void handleFailedResponse(final HttpURLConnection uc) throws IOException {
        throw new IOException(uc.getResponseMessage());
    }

    protected boolean hasFailed(final HttpURLConnection uc) throws IOException {
        return uc.getResponseCode() >= 400;
    }

}
