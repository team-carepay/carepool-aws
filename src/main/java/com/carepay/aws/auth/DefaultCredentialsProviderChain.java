package com.carepay.aws.auth;

import java.time.Clock;

/**
 * Default implementation of AWS credentials providers. Searches for credentials in the following
 * order: 1) Environment (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY) 2) System properties
 * (aws.accessKeyId / aws.secretAccessKey) 3) EC2 (using http://169.254.169.254/latest/meta-data)
 */
public class DefaultCredentialsProviderChain implements CredentialsProvider {
    private static final CredentialsProvider INSTANCE = new DefaultCredentialsProviderChain();
    private final CredentialsProvider[] providers;

    private final Clock clock;
    private Credentials cachedCredentials;

    public DefaultCredentialsProviderChain() {
        this(Clock.systemUTC(), new EnvironmentCredentialsProvider(),
                new SystemPropertyCredentialsProvider(),
                new WebIdentityTokenCredentialsProvider(),
                new ProfileCredentialsProvider(),
                new EC2CredentialsProvider()
        );
    }

    public DefaultCredentialsProviderChain(final Clock clock, final CredentialsProvider... providers) {
        this.clock = clock;
        this.providers = providers;
    }

    public static CredentialsProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public Credentials getCredentials() {
        if (cachedCredentials != null && (cachedCredentials.getExpiration() == null || cachedCredentials.getExpiration().isAfter(clock.instant()))) {
            return cachedCredentials;
        }
        for (CredentialsProvider provider : providers) {
            Credentials credentials = provider.getCredentials();
            if (credentials != null && credentials.isPresent()) {
                cachedCredentials = credentials;
                return credentials;
            }
        }
        return null;
    }
}
