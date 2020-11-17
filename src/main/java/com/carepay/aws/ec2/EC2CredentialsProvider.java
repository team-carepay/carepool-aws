package com.carepay.aws.ec2;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import com.carepay.aws.auth.Credentials;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.util.Env;
import com.carepay.aws.util.URLOpener;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2CredentialsProvider implements CredentialsProvider {
    static final String ECS_CONTAINER_CREDENTIALS_PATH = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    static final String CONTAINER_CREDENTIALS_FULL_URI = "AWS_CONTAINER_CREDENTIALS_FULL_URI";
    static final String CONTAINER_AUTHORIZATION_TOKEN = "AWS_CONTAINER_AUTHORIZATION_TOKEN";
    @SuppressWarnings("java:S1313")
    private static final String ECS_CREDENTIALS_ENDPOINT = "http://169.254.170.2";

    @SuppressWarnings("java:S1313")
    static final String SECURITY_CREDENTIALS_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/";

    private final ResourceFetcher resourceFetcher;
    private final Clock clock;
    final URL url;
    final Map<String, String> headers = new HashMap<>();
    private Credentials lastCredentials;

    public EC2CredentialsProvider() {
        this(new ResourceFetcher(new URLOpener.Default()), Clock.systemUTC(), new Env.Default());
    }

    public EC2CredentialsProvider(final ResourceFetcher resourceFetcher, final Clock clock, final Env env) {
        this.resourceFetcher = resourceFetcher;
        this.clock = clock;
        try {
            final String relativeUri = env.getEnv(ECS_CONTAINER_CREDENTIALS_PATH);
            if (relativeUri != null) {
                this.url = new URL(ECS_CREDENTIALS_ENDPOINT + relativeUri);
            } else {
                final String fullUri = env.getEnv(CONTAINER_CREDENTIALS_FULL_URI);
                if (fullUri != null) {
                    this.url = new URL(fullUri);
                    final String token = env.getEnv(CONTAINER_AUTHORIZATION_TOKEN);
                    if (token != null) {
                        headers.put("Authorization", token);
                    }
                } else {
                    final URL securityCredentialsUrl = new URL(SECURITY_CREDENTIALS_URL);
                    final String role = resourceFetcher.queryFirstLine(securityCredentialsUrl);
                    if (role != null) {
                        this.url = new URL(securityCredentialsUrl, role);
                    } else {
                        this.url = null;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public Credentials getCredentials() {
        if (url == null) {
            return null;
        }
        if (lastCredentials == null || (lastCredentials.getExpiration() != null && lastCredentials.getExpiration().isBefore(clock.instant()))) {
            Map<String, String> map = resourceFetcher.queryJson(url);
            lastCredentials = new Credentials(
                    map.get("AccessKeyId"),
                    map.get("SecretAccessKey"),
                    map.get("Token"),
                    map.get("Expiration")
            );
        }
        return lastCredentials;
    }
}
