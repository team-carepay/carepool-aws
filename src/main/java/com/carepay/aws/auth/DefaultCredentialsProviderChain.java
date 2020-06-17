package com.carepay.aws.auth;

import com.carepay.aws.ec2.EC2CredentialsProvider;

/**
 * Default implementation of AWS credentials providers. Searches for credentials in the following
 * order: 1) Environment (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY) 2) System properties
 * (aws.accessKeyId / aws.secretAccessKey) 3) EC2 (using http://169.254.169.254/latest/meta-data)
 */
public class DefaultCredentialsProviderChain implements CredentialsProvider {
    private static final CredentialsProvider INSTANCE = new DefaultCredentialsProviderChain();
    private final CredentialsProvider[] providers;

    public DefaultCredentialsProviderChain() {
        this(new EnvironmentCredentialsProvider(),
                new SystemPropertyCredentialsProvider(),
                new ProfileCredentialsProvider(),
                new EC2CredentialsProvider()
        );
    }

    public DefaultCredentialsProviderChain(CredentialsProvider... providers) {
        this.providers = providers;
    }

    public static CredentialsProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public Credentials getCredentials() {
        for (CredentialsProvider provider : providers) {
            Credentials credentials = provider.getCredentials();
            if (credentials != null && credentials.isPresent()) {
                return credentials;
            }
        }
        return null;
    }
}
