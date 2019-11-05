package com.carepay.aws;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemPropertiesAWSCredentialsProviderTest {

    private Properties oldProperties;
    private SystemPropertiesAWSCredentialsProvider credentialsProvider;

    @Before
    public void setUp() {
        oldProperties = System.getProperties();
        System.setProperty("aws.accessKeyId", "abc");
        System.setProperty("aws.secretAccessKey", "def");
        System.setProperty("aws.sessionToken", "ghi");
        credentialsProvider = new SystemPropertiesAWSCredentialsProvider();
    }

    @After
    public void tearDown() {
        System.setProperties(oldProperties);
    }

    @Test
    public void getCredentials() {
        final AWSCredentials credentials = credentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("abc");
    }
}
