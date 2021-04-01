package com.carepay.aws.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carepay.aws.util.Hex;
import com.carepay.aws.util.SHA256;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AWS4SignerTest {
    private static final byte[] TEST_BYTES = {(byte) 0x12, (byte) 0x34, (byte) 0xCA, (byte) 0xFE};
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
    private AWS4Signer signer;

    @Before
    public void setUp() {
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "SeSsIoNtOkEn");
        signer = new AWS4Signer("ec2", () -> credentials, () -> "eu-west-1", CLOCK);
    }

    @Test
    public void testHex() {
        assertThat(Hex.encode(TEST_BYTES)).isEqualTo("1234cafe");
    }

    @Test
    public void testHash() throws GeneralSecurityException {
        String hash = SHA256.hash("Testing123");
        assertThat(hash).isEqualTo("0218b3506b9b9de4fd357c0865a393471b73fc5ea972c5731219cfae32cee483");
        String hash2 = SHA256.hash("");
        assertThat(hash2).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    public void signShouldAddHeaders() throws IOException {
        URL url = new URL("https://ec2.eu-west-1.amazonaws.com/?Param2=value2&Param1=value1");
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getURL()).thenReturn(url);
        when(uc.getRequestMethod()).thenReturn("POST");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        signer.signHeaders(uc, null);
        verify(uc, times(4)).setRequestProperty(keyCaptor.capture(), valueCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        Map<String, String> headers = new HashMap<>();
        for (int n = 0; n < keys.size(); n++) {
            headers.put(keys.get(n), values.get(n));
        }
        assertThat(headers)
                .containsEntry("X-Amz-Date", "20180919T160242Z")
                .containsEntry("Authorization", "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20180919/eu-west-1/ec2/aws4_request, SignedHeaders=host;x-amz-date, Signature=9b1492651302e34ea30855a769377e4465a1cc96aedb5f81fd523e3c2dd3dad0")
                .containsEntry("X-Amz-Security-Token", "SeSsIoNtOkEn");
    }

    /**
     * https://docs.aws.amazon.com/general/latest/gr/signature-v4-test-suite.html
     */
    @Test
    public void aws4TestSuite() throws MalformedURLException {
        Clock clock = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), ZoneId.of("UTC"));
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        AWS4Signer aws4signer = new AWS4Signer("service", () -> credentials, () -> "us-east-1", clock);
        URL url = new URL("https://example.amazonaws.com/?Param1=value1&Param2=value2");
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getURL()).thenReturn(url);
        when(uc.getRequestMethod()).thenReturn("GET");
        aws4signer.signHeaders(uc, null);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(uc, times(3)).setRequestProperty(keyCaptor.capture(), valueCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        Map<String, String> headers = new HashMap<>();
        for (int n = 0; n < keys.size(); n++) {
            headers.put(keys.get(n), values.get(n));
        }
        assertThat(headers).containsEntry("Authorization","AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature=b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500");

    }

    @Test
    public void testMissingCredentials() throws MalformedURLException {
        AWS4Signer aws4signer = new AWS4Signer("ec2", () -> null, () -> "eu-west-1", CLOCK);
        URL url = new URL("https://example.amazonaws.com/?Param1=value1&Param2=value2");
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getURL()).thenReturn(url);
        when(uc.getRequestMethod()).thenReturn("GET");
        try {
            aws4signer.signHeaders(uc, null);
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
