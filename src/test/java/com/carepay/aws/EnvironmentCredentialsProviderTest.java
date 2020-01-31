package com.carepay.aws;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentCredentialsProviderTest {

    private EnvironmentCredentialsProvider environmentAWSCredentialsProvider;

    @Before
    public void setUp() {
        Properties environmentProperties = new Properties();
        environmentProperties.put("AWS_ACCESS_KEY_ID", "AKIDEXAMPLE");
        environmentProperties.put("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        environmentProperties.put("AWS_SESSION_TOKEN", "MySeSsIoNkEy");
        environmentAWSCredentialsProvider = new EnvironmentCredentialsProvider(environmentProperties::getProperty);
    }

    @Test
    public void testGetCredentials() {
        Credentials credentials = environmentAWSCredentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }
}
