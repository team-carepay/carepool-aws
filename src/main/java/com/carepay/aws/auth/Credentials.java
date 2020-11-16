package com.carepay.aws.auth;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Contains AWS Credentials.
 */
public class Credentials {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String token;
    private final Instant expiration;

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token) {
        this(accessKeyId, secretAccessKey, token, (Instant) null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token,
                       final String expiration) {
        this(accessKeyId, secretAccessKey, token, expiration != null ? DateTimeFormatter.ISO_INSTANT.parse(expiration, Instant::from) : null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token,
                       final Instant expiration) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.token = token;
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

    public String getToken() {
        return token;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public boolean hasToken() {
        return this.token != null;
    }
}
