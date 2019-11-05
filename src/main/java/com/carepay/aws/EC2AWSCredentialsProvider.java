package com.carepay.aws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;

import static java.time.ZoneOffset.UTC;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2AWSCredentialsProvider implements AWSCredentialsProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    protected static final String SECURITY_CREDENTIALS_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/";
    private final Clock clock;
    private final URL url;
    private AWSCredentials lastCredentials;
    private LocalDateTime expiryDate;
    private URLOpener opener;

    public EC2AWSCredentialsProvider() {
        this(Clock.systemUTC(), URLOpener.DEFAULT);
    }

    public EC2AWSCredentialsProvider(final Clock clock, final URLOpener opener) {
        this(clock, determineCredentialsURL(opener), opener);
    }

    public EC2AWSCredentialsProvider(final Clock clock, final URL url, final URLOpener opener) {
        this.clock = clock;
        this.url = url;
        this.opener = opener;
    }

    private static URL determineCredentialsURL(URLOpener opener) {
        try {
            URL url = new URL(SECURITY_CREDENTIALS_URL);
            String role = queryMetaDataAsString(url, opener);
            return role != null ? new URL(url, role) : null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(),UTC);
        if (url != null && (expiryDate == null || expiryDate.isBefore(now))) {
            Map<String, String> map = queryMetaData(url, opener);
            lastCredentials = new AWSCredentials(map.get("AccessKeyId"), map.get("SecretAccessKey"), map.get("Token"));
            final String expirationString = map.get("Expiration");
            if (lastCredentials.isValid() && expirationString != null) {
                expiryDate = LocalDateTime.parse(expirationString, DATE_FORMATTER);
            }
        }
        return lastCredentials;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> queryMetaData(final URL url, final URLOpener opener) {
        try {
            final HttpURLConnection urlConnection = opener.open(url);
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            try (final InputStream is = urlConnection.getInputStream();
                 final InputStreamReader reader = new InputStreamReader(is)) {
                return (Map<String, String>) Jsoner.deserialize(reader);
            }
        } catch (IOException | JsonException e) { // NOSONAR
            return Collections.emptyMap();
        }
    }

    public static String queryMetaDataAsString(final URL url, final URLOpener opener) {
        try {
            final HttpURLConnection urlConnection = opener.open(url);
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            try (final InputStream is = urlConnection.getInputStream();
                 final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                final byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) {
                    baos.write(buf, 0, n);
                }
                return baos.toString();
            }
        } catch (IOException e) { // NOSONAR
            return null;
        }
    }

}
