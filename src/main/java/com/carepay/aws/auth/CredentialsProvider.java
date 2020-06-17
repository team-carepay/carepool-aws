package com.carepay.aws.auth;

/**
 * Provides access to AWS credentials
 */
public interface CredentialsProvider {
    Credentials getCredentials();
}
