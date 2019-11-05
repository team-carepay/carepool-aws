package com.carepay.aws;

/**
 * Default implementation of AWS credentials providers. Searches for credentials in the following
 * order: 1) Environment (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY) 2) System properties
 * (aws.accessKeyId / aws.secretAccessKey) 3) EC2 (using http://169.254.169.254/latest/meta-data)
 */
public class DefaultAWSCredentialsProviderChain implements AWSCredentialsProvider {
    private static final AWSCredentialsProvider[] providers = new AWSCredentialsProvider[]{
            new EnvironmentAWSCredentialsProvider(),
            new SystemPropertiesAWSCredentialsProvider(),
            new ProfileAWSCredentialsProvider(),
            new EC2AWSCredentialsProvider()
    };

    @Override
    public AWSCredentials getCredentials() {
        for (AWSCredentialsProvider provider : providers) {
            AWSCredentials credentials = provider.getCredentials();
            if (credentials.isValid()) {
                return credentials;
            }
        }
        return null;
    }
}
