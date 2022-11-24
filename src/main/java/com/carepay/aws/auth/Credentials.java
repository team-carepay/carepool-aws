package com.carepay.aws.auth;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Contains AWS Credentials.
 */
public class Credentials {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;
    private final Instant expiration;

    public Credentials() {
        this(null, null, null, (Instant) null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String sessionToken) {
        this(accessKeyId, secretAccessKey, sessionToken, (Instant) null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String sessionToken,
                       final String expiration) {
        this(accessKeyId, secretAccessKey, sessionToken, expiration != null ? DateTimeFormatter.ISO_INSTANT.parse(expiration, Instant::from) : null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String sessionToken,
                       final Instant expiration) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.expiration = expiration;
    }

    public boolean isPresent() {
        return accessKeyId != null && secretAccessKey != null;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public boolean hasToken() {
        return this.sessionToken != null;
    }
}
