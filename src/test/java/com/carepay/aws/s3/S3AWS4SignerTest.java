package com.carepay.aws.s3;

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

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.Credentials;
import com.carepay.aws.net.URLOpener;
import com.carepay.aws.util.Hex;
import com.carepay.aws.util.SHA256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3AWS4SignerTest {
    private static final byte[] TEST_BYTES = {(byte) 0x12, (byte) 0x34, (byte) 0xCA, (byte) 0xFE};
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
    private AWS4Signer signer;

    @BeforeEach
    public void setUp() {
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "SeSsIoNtOkEn");
        signer = new AWS4Signer("s3", () -> credentials, () -> "eu-west-1", CLOCK);
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

    /**
     * Generated from the test-suite
     */
    @Test
    public void signQueryShouldAddParams() throws IOException {
        Clock myClock = Clock.fixed(Instant.parse("2022-08-26T20:25:57Z"), ZoneId.of("UTC"));
        Credentials myCredentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        AWS4Signer mySigner = new AWS4Signer("s3", () -> myCredentials, () -> "eu-west-1", myClock);
        URL url = new URL("https://s3-eu-west-1.amazonaws.com/test-bucket/ExampleObject.txt");
        URL signedUrl = mySigner.preSign(url);
        assertThat(signedUrl).isEqualTo(URLOpener.create("https://s3-eu-west-1.amazonaws.com/test-bucket/ExampleObject.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20220826%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Date=20220826T202557Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=6877c09905535205a6d4ccbcc246e5aa1f30c237b08dd8843d79739710cd5e9d"));
    }

    @Test
    public void signQueryWithSessionToken() throws IOException {
        Clock myClock = Clock.fixed(Instant.parse("2022-08-26T20:25:57Z"), ZoneId.of("UTC"));
        Credentials myCredentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "SeSsIoNtOkEn");
        AWS4Signer mySigner = new AWS4Signer("s3", () -> myCredentials, () -> "eu-west-1", myClock);
        URL url = new URL("https://s3-eu-west-1.amazonaws.com/test-bucket/ExampleObject.txt");
        URL signedUrl = mySigner.preSign(url);
        assertThat(signedUrl).isEqualTo(URLOpener.create("https://s3-eu-west-1.amazonaws.com/test-bucket/ExampleObject.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20220826%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Date=20220826T202557Z&X-Amz-Expires=604800&X-Amz-Security-Token=SeSsIoNtOkEn&X-Amz-SignedHeaders=host&X-Amz-Signature=201187464e789804e43f97c15c45287d2a4bb06d8be41cb4b5f8c0b9f49e0c46"));
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
        assertThat(headers).containsEntry("Authorization", "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature=b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500");

    }

    @Test
    public void testMissingCredentials() throws MalformedURLException {
        AWS4Signer aws4signer = new AWS4Signer("ec2", () -> null, () -> "eu-west-1", CLOCK);
        URL url = new URL("https://example.amazonaws.com/?Param1=value1&Param2=value2");
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getURL()).thenReturn(url);
        when(uc.getRequestMethod()).thenReturn("GET");
        assertThatThrownBy(() -> {
            aws4signer.signHeaders(uc, null);
        }).isInstanceOf(IllegalStateException.class);
    }
}
