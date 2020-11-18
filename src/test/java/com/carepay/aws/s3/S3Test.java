package com.carepay.aws.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.Credentials;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3Test {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);

    private S3 s3;
    private HttpURLConnection urlConnection;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws IOException {
        urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getURL()).thenReturn(new URL("https://testbucket.s3.us-east-1.amazonaws.com/testkey.png"));
        outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new S3AWS4Signer(() -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        s3 = new S3(signer, (u) -> urlConnection);
    }

    @Test
    public void testPutObject() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("PUT");
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{0x01}));
        s3.putObject("testbucket", "testkey.png", new byte[]{0x01, 0x02, 0x03, (byte) 0x81}, 0, 4);
        assertThat(outputStream.toByteArray()).hasSize(4);
    }

    @Test
    public void testPutObjectError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("<Error><Message>Test</Message></Error>".getBytes(UTF_8)));
        try {
            s3.putObject("testbucket", "testkey.png", new byte[]{0x01}, 0, 1);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Test");
        }
    }

    @Test
    public void testPutObjectErrorInvalidXml() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Broken".getBytes(UTF_8)));
        try {
            s3.putObject("testbucket", "testkey.png", new byte[]{0x01}, 0, 1);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).containsIgnoringCase("xml");
        }
    }

    @Test
    public void testMultipart() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST", "PUT", "POST");
        when(urlConnection.getInputStream()).thenReturn(
                getClass().getResourceAsStream("/s3-create-multipart-response.xml"),
                getClass().getResourceAsStream("/s3-upload-part-response.xml"),
                getClass().getResourceAsStream("/s3-complete-multipart-response.xml")
        );
        when(urlConnection.getHeaderField(eq("ETag"))).thenReturn("b54357faf0632cce46e942fa68356b38");
        ByteArrayOutputStream uploadPartRequest = new ByteArrayOutputStream();
        ByteArrayOutputStream finishRequest = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(uploadPartRequest, finishRequest);
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        s3.uploadPart(uploadId, "ABCD".getBytes(UTF_8), 0, 4);
        assertThat(uploadPartRequest).hasToString("ABCD");
        s3.finishMultipart(uploadId);
        assertThat(finishRequest).hasToString("<CompleteMultipartUpload xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Part><ETag>b54357faf0632cce46e942fa68356b38</ETag><PartNumber>1</PartNumber></Part></CompleteMultipartUpload>");
    }

    @Test
    public void testStartMultipartError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("<Error><Message>Test</Message></Error>".getBytes(UTF_8)));
        try {
            s3.startMultipart("testbucket", "testkey2.png");
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Test");
        }
    }

    @Test
    public void testStartMultipartErrorInvalidXml() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Broken".getBytes(UTF_8)));
        try {
            s3.startMultipart("testbucket", "testkey2.png");
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).containsIgnoringCase("xml");
        }
    }

    @Test
    public void abortMultipart() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST", "DELETE");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        s3.abortMultipartUpload(uploadId);
        verify(urlConnection, times(2)).getRequestMethod();
    }

    @Test
    public void abortMultipartError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("DELETE");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("<Error><Message>Test</Message></Error>".getBytes(UTF_8)));
        try {
            s3.abortMultipartUpload(uploadId);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Test");
        }
    }

    @Test
    public void abortMultipartErrorInvalidXml() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("DELETE");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Invalid".getBytes(UTF_8)));
        try {
            s3.abortMultipartUpload(uploadId);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).containsIgnoringCase("xml");
        }
    }

    @Test
    public void uploadPartError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("PUT");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("<Error><Message>Test</Message></Error>".getBytes(UTF_8)));
        try {
            s3.uploadPart(uploadId, new byte[]{1}, 0, 1);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Test");
        }
    }

    @Test
    public void uploadPartErrorInvalidXml() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getRequestMethod()).thenReturn("PUT");
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Invalid".getBytes(UTF_8)));
        try {
            s3.uploadPart(uploadId, new byte[]{1}, 0, 1);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).containsIgnoringCase("xml");
        }
    }

    @Test
    public void finishMultipartError() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("<Error><Message>Test</Message></Error>".getBytes(UTF_8)));
        try {
            s3.finishMultipart(uploadId);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Test");
        }
    }

    @Test
    public void finishMultipartErrorInvalidXml() throws IOException {
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/s3-create-multipart-response.xml"));
        String uploadId = s3.startMultipart("testbucket", "testkey2.png");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("Invalid".getBytes(UTF_8)));
        try {
            s3.finishMultipart(uploadId);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).containsIgnoringCase("xml");
        }
    }
}
