package com.carepay.aws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import static com.carepay.aws.EC2AWSCredentialsProvider.SECURITY_CREDENTIALS_URL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2AWSCredentialsProviderTest {
    private EC2AWSCredentialsProvider credentialsProvider;

    @Before
    public void setUp() throws IOException {
        Clock clock = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
        HttpURLConnection ucRole = mock(HttpURLConnection.class);
        when(ucRole.getInputStream()).thenReturn(getClass().getResourceAsStream("/security-credentials-role.txt"));
        HttpURLConnection ucJson = mock(HttpURLConnection.class);
        when(ucJson.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-credentials.json"));
        credentialsProvider = new EC2AWSCredentialsProvider(clock, url -> {
            if (SECURITY_CREDENTIALS_URL.equals(url.toString())) {
                return ucRole;
            } else {
                return ucJson;
            }
        });
    }

    @Test
    public void testGetCredentials() {
        AWSCredentials creds = credentialsProvider.getCredentials();
        assertThat(creds).isNotNull();
        assertThat(creds.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }
}
