package com.carepay.aws;

public class AWSCredentials {
    public static final AWSCredentials NULL = new AWSCredentials(null, null, null);

    private String accessKeyId;
    private String secretAccessKey;
    private String token;

    public AWSCredentials(String accessKeyId, String secretAccessKey, String token) {
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
