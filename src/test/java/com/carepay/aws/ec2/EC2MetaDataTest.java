package com.carepay.aws.ec2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2MetaDataTest {
    @Test
    public void testGetInstanceId() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/metadata.json"));
        EC2MetaData ec2metadata = new EC2MetaData(url -> uc);
        assertThat(ec2metadata.getInstanceId()).isEqualTo("i-05c52026d927fee31");
    }

    @Test
    public void testCreateURL() throws MalformedURLException {
        assertThat(EC2MetaData.createURL("https://host.com")).isEqualTo(new URL("https://host.com"));
        try {
            EC2MetaData.createURL("johndoe");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testQueryMetadataIOException() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenThrow(new IOException("test"));
        EC2MetaData ec2metadata = new EC2MetaData(url -> uc);
        assertThat(ec2metadata.queryMetaData(new URL("https://localhost"))).isEmpty();
    }

    @Test
    public void testQueryMetadataError() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(uc.getResponseCode()).thenReturn(400);
        EC2MetaData ec2metadata = new EC2MetaData(url -> uc);
        assertThat(ec2metadata.queryMetaDataAsString(new URL("https://localhost"))).isNull();
    }

    @Test
    public void testQueryMetadataAsStringIOException() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenThrow(new IOException("test"));
        EC2MetaData ec2metadata = new EC2MetaData(url -> uc);
        assertThat(ec2metadata.queryMetaDataAsString(new URL("https://localhost"))).isNull();
    }

}
