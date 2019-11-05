package com.carepay.aws;

/**
 * Provides access to AWS credentials
 */
public interface AWSCredentialsProvider {
    AWSCredentials getCredentials();
}
