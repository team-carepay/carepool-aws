package com.carepay.aws;

/**
 * Provides access to AWS credentials
 */
public interface CredentialsProvider {
    Credentials getCredentials();
}
