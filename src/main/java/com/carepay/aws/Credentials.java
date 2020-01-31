package com.carepay.aws;

/**
 * Contains AWS Credentials.
 */
public class Credentials {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String token;

    public Credentials(String accessKeyId, String secretAccessKey, String token) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.token = token;
    }

    public boolean isValid() {
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

    public boolean hasToken() {
        return this.token != null;
    }
}
