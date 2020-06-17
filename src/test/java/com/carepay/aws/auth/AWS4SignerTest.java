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
        AWS4Signer.KEY_CACHE.clear();
        signer = new AWS4Signer(() -> credentials, () -> "eu-west-1", CLOCK);
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
        signer.sign("ec2", uc, null);
        verify(uc, times(4)).setRequestProperty(keyCaptor.capture(), valueCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        Map<String, String> headers = new HashMap<>();
        for (int n = 0; n < keys.size(); n++) {
            headers.put(keys.get(n), values.get(n));
        }
        assertThat(headers.get("X-Amz-Date")).isEqualTo("20180919T160242Z");
        assertThat(headers.get("Authorization")).isEqualTo("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20180919/eu-west-1/ec2/aws4_request, SignedHeaders=host;x-amz-date, Signature=9b1492651302e34ea30855a769377e4465a1cc96aedb5f81fd523e3c2dd3dad0");
        assertThat(headers.get("X-Amz-Security-Token")).isEqualTo("SeSsIoNtOkEn");
    }

    @Test
    public void testGeneratorSignature() throws MalformedURLException, GeneralSecurityException {
        String token = signer.createDbAuthToken("dbhost.xyz.eu-west-1.amazonaws.com", 3306, "iam_user");
        assertThat(token).isEqualTo("dbhost.xyz.eu-west-1.amazonaws.com:3306/?Action=connect&DBUser=iam_user&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=SeSsIoNtOkEn&X-Amz-SignedHeaders=host&X-Amz-Signature=0c5925dc554b2e175d58dcb96d9ad71cf85c81ba0835a523291f9f6ce90096ca");
    }

    @Test
    public void testRealRdsToken() throws MalformedURLException, GeneralSecurityException {
        Clock clock = Clock.fixed(Instant.parse("2020-01-28T01:17:52.00Z"), ZoneId.of("UTC"));
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        AWS4Signer aws4signer = new AWS4Signer(() -> credentials, () -> "eu-west-1", clock);

        String token = aws4signer.createDbAuthToken("rdsmysql.cdgmuqiadpid.us-west-2.rds.amazonaws.com", 3306, "jane_doe");
        assertThat(token).isEqualTo("rdsmysql.cdgmuqiadpid.us-west-2.rds.amazonaws.com:3306/?Action=connect&DBUser=jane_doe&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20200128%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20200128T011752Z&X-Amz-Expires=900&X-Amz-SignedHeaders=host&X-Amz-Signature=9fa655ff59886494ffce521cb732648b9dd8feeb71b9b14d3d1e2b8a3c500ce8");
    }

    /**
     * https://docs.aws.amazon.com/general/latest/gr/signature-v4-test-suite.html
     */
    @Test
    public void aws4TestSuite() throws MalformedURLException {
        Clock clock = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), ZoneId.of("UTC"));
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        AWS4Signer aws4signer = new AWS4Signer(() -> credentials, () -> "us-east-1", clock);
        URL url = new URL("https://example.amazonaws.com/?Param1=value1&Param2=value2");
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getURL()).thenReturn(url);
        when(uc.getRequestMethod()).thenReturn("GET");
        aws4signer.sign("service", uc, null);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(uc, times(3)).setRequestProperty(keyCaptor.capture(), valueCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        Map<String, String> headers = new HashMap<>();
        for (int n = 0; n < keys.size(); n++) {
            headers.put(keys.get(n), values.get(n));
        }
        assertThat(headers.get("Authorization")).isEqualTo("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature=b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500");

    }
}
