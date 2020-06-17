package com.carepay.aws.auth;

import java.util.Map;

public class StaticProfileCredentialsProvider implements CredentialsProvider {
    private static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    private static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    private static final String AWS_SESSION_TOKEN = "aws_session_token";
    private final Map<String, String> section;

    public StaticProfileCredentialsProvider(final Map<String, String> section) {
        this.section = section;
    }

    @Override
    public Credentials getCredentials() {
        final String accessKeyId = section.get(AWS_ACCESS_KEY_ID);
        final String secretAccessKey = section.get(AWS_SECRET_ACCESS_KEY);
        return accessKeyId != null && secretAccessKey != null ? new Credentials(
                accessKeyId,
                secretAccessKey,
                section.get(AWS_SESSION_TOKEN)
        ) : null;
    }
}
