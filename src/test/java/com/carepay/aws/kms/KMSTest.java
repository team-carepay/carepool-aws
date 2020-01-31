package com.carepay.aws.kms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.AWS4Signer;
import com.carepay.aws.Credentials;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KMSTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);

    @Test
    public void testEncrypt() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getRequestMethod()).thenReturn("GET");
        when(uc.getResponseCode()).thenReturn(200);
        when(uc.getURL()).thenReturn(new URL("https://kms.us-east-1.amazonaws.com/?"));
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/kms-encrypt-response.json"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(uc.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer(() -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        KMS kms = new KMS(signer, u -> uc);
        String cipherText = kms.encrypt("PlainText", "test-key-id");
        assertThat(cipherText).isEqualTo("CiDPoCH188S65r5Cy7pAhIFJMXDlU7mewhSlYUpuQIVBrhKmAQEBAgB4z6Ah9fPEuua+Qsu6QISBSTFw5VO5nsIUpWFKbkCFQa4AAAB9MHsGCSqGSIb3DQEHBqBuMGwCAQAwZwYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAxLc9b6QThC9jB/ZjYCARCAOt8la8qXLO5wB3JH2NlwWWzWRU2RKqpO9A/0psE5UWwkK6CnwoeC3Zj9Q0A66apZkbRglFfY1lTY+Tc=");
    }

    @Test
    public void testDecrypt() throws IOException {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getResponseCode()).thenReturn(200);
        when(urlConnection.getURL()).thenReturn(new URL("https://kms.us-east-1.amazonaws.com/?"));
        when(urlConnection.getInputStream()).thenReturn(getClass().getResourceAsStream("/kms-decrypt-response.json"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer(() -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        KMS kms = new KMS(signer, u -> urlConnection);
        String plainText = kms.decrypt("CiDPoCH188S65r5Cy7pAhIFJMXDlU7mewhSlYUpuQIVBrhKmAQEBAgB4z6Ah9fPEuua+Qsu6QISBSTFw5VO5nsIUpWFKbkCFQa4AAAB9MHsGCSqGSIb3DQEHBqBuMGwCAQAwZwYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAxLc9b6QThC9jB/ZjYCARCAOt8la8qXLO5wB3JH2NlwWWzWRU2RKqpO9A/0psE5UWwkK6CnwoeC3Zj9Q0A66apZkbRglFfY1lTY+Tc=");
        assertThat(plainText).isEqualTo("Plaintext");
    }

    @Test
    public void testDecryptWithError() throws IOException {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getRequestMethod()).thenReturn("GET");
        when(urlConnection.getResponseCode()).thenReturn(400);
        when(urlConnection.getURL()).thenReturn(new URL("https://kms.us-east-1.amazonaws.com/?"));
        when(urlConnection.getErrorStream()).thenReturn(getClass().getResourceAsStream("/kms-decrypt-error-response.json"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        AWS4Signer signer = new AWS4Signer(() -> new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null), () -> "us-east-1", CLOCK);
        KMS kms = new KMS(signer, u -> urlConnection);
        try {
            kms.decrypt("CiDPoCH188S65r5Cy7pAhIFJMXDlU7mewhSlYUpuQIVBrhKmAQEBAgB4z6Ah9fPEuua+Qsu6QISBSTFw5VO5nsIUpWFKbkCFQa4AAAB9MHsGCSqGSIb3DQEHBqBuMGwCAQAwZwYJKoZIhvcNAQcBMB4GCWCGSAFlAwQBLjARBAxLc9b6QThC9jB/ZjYCARCAOt8la8qXLO5wB3JH2NlwWWzWRU2RKqpO9A/0psE5UWwkK6CnwoeC3Zj9Q0A66apZkbRglFfY1lTY+Tc=");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Something bad happened");
        }
    }
}
