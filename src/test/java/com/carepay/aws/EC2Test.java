package com.carepay.aws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2Test {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
    private EC2 ec2;
    private HttpURLConnection urlConnection;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws IOException {
        urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getURL()).thenReturn(new URL("https://ec2.us-east-1.amazonaws.com/?"));
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-describe-tags-response.xml"));
        outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer(CLOCK, () -> new AWSCredentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null));
        ec2 = new EC2(signer, u -> urlConnection);
    }

    @Test
    public void testGetTags() {
        Map<String, String> tags = ec2.describeTags("eu-west-1", "i-12345");
        assertThat(tags.size()).isEqualTo(7);
    }

    @Test
    public void testGetInstanceId() throws IOException {
        HttpURLConnection metadataURLConnection = mock(HttpURLConnection.class);
        when(metadataURLConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/metadata.json"));
        Map<String,String> metadata = EC2.queryMetaData(new URL("http://anyhost/"), u -> metadataURLConnection);
        assertThat(metadata.get("instanceId")).isEqualTo("i-05c52026d927fee31");
    }
}
