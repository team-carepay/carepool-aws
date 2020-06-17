package com.carepay.aws.auth;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Contains AWS Credentials.
 */
public class Credentials {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String token;
    private final LocalDateTime expiration;

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token) {
        this(accessKeyId, secretAccessKey, token, (LocalDateTime) null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token,
                       final String expiration) {
        this(accessKeyId, secretAccessKey, token, expiration != null ? LocalDateTime.parse(expiration, DATE_TIME_FORMATTER) : null);
    }

    public Credentials(final String accessKeyId,
                       final String secretAccessKey,
                       final String token,
                       final LocalDateTime expiration) {
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

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public boolean hasToken() {
        return this.token != null;
    }
}
