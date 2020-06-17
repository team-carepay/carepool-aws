package com.carepay.aws.auth;

import java.util.Optional;

/**
 * Credentials provider which uses Java System Properties (aws.accessKeyId and aws.secretAccessKey)
 */
public class SystemPropertyCredentialsProvider implements CredentialsProvider {
    @Override
    public Credentials getCredentials() {
        return new Credentials(
                System.getProperty("aws.accessKeyId"),
                Optional.ofNullable(System.getProperty("aws.secretAccessKey")).orElseGet(() -> System.getProperty("aws.secretKey")),
                Optional.ofNullable(System.getProperty("aws.sessionToken")).orElseGet(() -> System.getProperty("aws.token"))
        );
    }
}
