package com.carepay.aws.auth;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.carepay.aws.net.FirstLineResponseReader;
import com.carepay.aws.net.JsonResponseReader;
import com.carepay.aws.net.URLOpener;
import com.carepay.aws.net.WebClient;
import com.carepay.aws.util.Env;

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

    private final WebClient webClient;
    final URL url;
    final Map<String, String> headers = new HashMap<>();
    private static final JsonResponseReader<EC2Credentials> JSON_RESPONSE_READER = new JsonResponseReader<>(EC2Credentials.class);


    public EC2CredentialsProvider() {
        this(new WebClient(new URLOpener.Default()), new Env.Default());
    }

    public EC2CredentialsProvider(final WebClient webClient, final Env env) {
        this.webClient = webClient;
        URL credentialsUrl = null;
        try {
            final String relativeUri = env.getEnv(ECS_CONTAINER_CREDENTIALS_PATH);
            if (relativeUri != null) {
                credentialsUrl = new URL(ECS_CREDENTIALS_ENDPOINT + relativeUri);
            } else {
                final String fullUri = env.getEnv(CONTAINER_CREDENTIALS_FULL_URI);
                if (fullUri != null) {
                    credentialsUrl = new URL(fullUri);
                    final String token = env.getEnv(CONTAINER_AUTHORIZATION_TOKEN);
                    if (token != null) {
                        headers.put("Authorization", token);
                    }
                } else {
                    final URL securityCredentialsUrl = new URL(SECURITY_CREDENTIALS_URL);
                    final String role = webClient.execute("GET", securityCredentialsUrl, null, new FirstLineResponseReader(), null);
                    if (role != null) {
                        credentialsUrl = new URL(securityCredentialsUrl, role);
                    }
                }
            }
        } catch (IOException e) {
            // ignore any exception
        }
        this.url = credentialsUrl;
    }

    @Override
    public Credentials getCredentials() {
        if (url != null) {
            try {
                final EC2Credentials ec2Credentials = webClient.execute("GET", url, null, JSON_RESPONSE_READER, headers);
                return ec2Credentials;
            } catch (IOException e) {
                e.printStackTrace();
                // ignore
            }
        }
        return null;
    }
}
