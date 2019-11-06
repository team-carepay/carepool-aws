package com.carepay.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2AWSCredentialsProvider implements AWSCredentialsProvider {
    protected static final String SECURITY_CREDENTIALS_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
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
            String role = EC2.queryMetaDataAsString(url, opener);
            return role != null ? new URL(url, role) : null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (url != null && (expiryDate == null || expiryDate.isBefore(now))) {
            Map<String, String> map = EC2.queryMetaData(url, opener);
            lastCredentials = new AWSCredentials(map.get("AccessKeyId"), map.get("SecretAccessKey"), map.get("Token"));
            final String expirationString = map.get("Expiration");
            if (lastCredentials.isValid() && expirationString != null) {
                expiryDate = LocalDateTime.parse(expirationString, DATE_FORMATTER);
            }
        }
        return lastCredentials;
    }
}
