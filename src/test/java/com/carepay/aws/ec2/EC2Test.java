package com.carepay.aws.ec2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2Test {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
    private EC2 ec2;
    private HttpURLConnection urlConnection;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setUp() throws IOException {
        urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getURL()).thenReturn(new URL("https://ec2.us-east-1.amazonaws.com/?"));
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-describe-tags-response.xml"));
        outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer("ec2", () -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        ec2 = new EC2(signer, u -> urlConnection);
    }

    @Test
    public void testDescribeTags() throws IOException {
        Map<String, String> tags = ec2.describeTags("i-12345");
        assertThat(tags).hasSize(7);
    }

    @Test
    public void testDescribeTagsError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getErrorStream()).thenReturn(getClass().getResourceAsStream("/ec2-describe-tags-error.xml"));
        assertThatThrownBy(() -> ec2.describeTags("123"))
                .isInstanceOf(IOException.class)
                .hasMessage("AWS was not able to validate the provided access credentials");
    }

    @Test
    public void testDescribeTagsIOException() throws IOException {
        when(urlConnection.getInputStream()).thenThrow(new IOException("test"));
        assertThatThrownBy(() -> ec2.describeTags("123"))
                .isInstanceOf(IOException.class)
                .hasMessage("test");
    }

    @Test
    public void testGetInstanceId() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/metadata.json"));
        EC2 ec2 = new EC2(mock(AWS4Signer.class), url -> uc);
        assertThat(ec2.queryMetaData().getInstanceId()).isEqualTo("i-05c52026d927fee31");
    }

    @Test
    public void testQueryMetadataIOException() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenThrow(new IOException("test"));
        EC2 ec2 = new EC2(mock(AWS4Signer.class), url -> uc);
        assertThatThrownBy(() -> ec2.queryMetaData().getInstanceId()).isInstanceOf(IOException.class);
    }

    @Test
    public void testQueryMetadataError() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(uc.getResponseCode()).thenReturn(400);
        EC2 ec2 = new EC2(mock(AWS4Signer.class), url -> uc);
        assertThatThrownBy(() -> ec2.queryMetaData().getInstanceId()).isInstanceOf(IOException.class);
    }

}
