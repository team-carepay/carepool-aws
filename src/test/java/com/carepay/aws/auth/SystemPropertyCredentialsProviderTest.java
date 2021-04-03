package com.carepay.aws.auth;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemPropertyCredentialsProviderTest {

    private Properties oldProperties;
    private SystemPropertyCredentialsProvider credentialsProvider;

    @BeforeEach
    public void setUp() {
        oldProperties = System.getProperties();
        System.setProperty("aws.accessKeyId", "abc");
        System.setProperty("aws.secretAccessKey", "def");
        System.setProperty("aws.sessionToken", "ghi");
        credentialsProvider = new SystemPropertyCredentialsProvider();
    }

    @AfterEach
    public void tearDown() {
        System.setProperties(oldProperties);
    }

    @Test
    public void getCredentials() {
        final Credentials credentials = credentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("abc");
    }
}
