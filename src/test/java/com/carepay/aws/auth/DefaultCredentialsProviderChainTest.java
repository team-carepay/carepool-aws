package com.carepay.aws.auth;

import java.util.Optional;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCredentialsProviderChainTest {

    private Properties oldProperties;
    private DefaultCredentialsProviderChain credentialsProviderChain;

    @Before
    public void setUp() {
        oldProperties = System.getProperties();
        System.setProperty("aws.accessKeyId", "abc");
        System.setProperty("aws.secretAccessKey", "def");
        System.setProperty("aws.sessionToken", "ghi");
        credentialsProviderChain = new DefaultCredentialsProviderChain();
    }

    @After
    public void tearDown() {
        System.setProperties(oldProperties);
    }

    @Test
    public void getCredentials() {
        final Credentials credentials = credentialsProviderChain.getCredentials();
        final String accessKey = Optional.ofNullable(System.getenv("AWS_ACCESS_KEY_ID")).orElse("abc");
        assertThat(credentials.getAccessKeyId()).isEqualTo(accessKey);
    }

    @Test
    public void cannotFindProviders() {
        DefaultCredentialsProviderChain chain = new DefaultCredentialsProviderChain(() -> null);
        assertThat(chain.getCredentials()).isNull();
    }
}
