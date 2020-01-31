package com.carepay.aws.ec2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.carepay.aws.AWS4Signer;
import com.carepay.aws.Credentials;
import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getURL()).thenReturn(new URL("https://ec2.us-east-1.amazonaws.com/?"));
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/ec2-describe-tags-response.xml"));
        outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer(() -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        ec2 = new EC2(signer, () -> "us-east-1", u -> urlConnection, XPathFactory.newInstance().newXPath());
    }

    @Test
    public void testDescribeTags() {
        Map<String, String> tags = ec2.describeTags("i-12345");
        assertThat(tags.size()).isEqualTo(7);
    }

    @Test
    public void testDescribeTagsError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        try {
            ec2.describeTags("123");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDescribeTagsIOException() throws IOException {
        when(urlConnection.getInputStream()).thenThrow(new IOException("test"));
        try {
            ec2.describeTags("123");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testDescribeTagsXpathException() throws XPathExpressionException {
        XPath xpath = mock(XPath.class);
        when(xpath.compile(anyString())).thenThrow(new XPathExpressionException("test"));
        try {
            new EC2(null, () -> "us-east-1", u -> urlConnection, xpath);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
