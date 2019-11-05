package com.carepay.aws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private AWS4Signer signer;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));

    @Before
    public void setUp() {
        AWSCredentials credentials = new AWSCredentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "SeSsIoNtOkEn");
        signer = new AWS4Signer(CLOCK, () -> credentials);
    }

    @Test
    public void testHex() {
        assertThat(AWS4Signer.hex(TEST_BYTES)).isEqualTo("1234cafe");
    }

    @Test
    public void testHash() throws GeneralSecurityException {
        String hash = AWS4Signer.hash("Testing123");
        assertThat(hash).isEqualTo("0218b3506b9b9de4fd357c0865a393471b73fc5ea972c5731219cfae32cee483");
        String hash2 = AWS4Signer.hash("");
        assertThat(hash2).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    public void signShouldAddHeaders() throws IOException {
        URL url = new URL("https://ec2.us-east-1.amazonaws.com/?Param2=value2&Param1=value1");
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(urlConnection.getURL()).thenReturn(url);
        when(urlConnection.getRequestMethod()).thenReturn("POST");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        signer.sign(urlConnection, "");
        verify(urlConnection, times(3)).setRequestProperty(keyCaptor.capture(), valueCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        Map<String, String> headers = new HashMap<>();
        for (int n = 0; n < keys.size(); n++) {
            headers.put(keys.get(n), values.get(n));
        }
        assertThat(headers.get("X-Amz-Date")).isEqualTo("20180919T160242Z");
        assertThat(headers.get("Authorization")).isEqualTo("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20180919/us-east-1/ec2/aws4_request, SignedHeaders=host;x-amz-date, Signature=e7286cad4bf301b0e8b13cde2fdc889007522984a12adbb4a62bc9a259330b30");
        assertThat(headers.get("X-Amz-Security-Token")).isEqualTo("SeSsIoNtOkEn");
    }

    @Test
    public void testGeneratorSignature() {
        String token = signer.createDbAuthToken("dbhost.xyz.eu-west-1.amazonaws.com", 3306, "iam_user");
        assertThat(token).isEqualTo("dbhost.xyz.eu-west-1.amazonaws.com:3306/?Action=connect&DBUser=iam_user&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=SeSsIoNtOkEn&X-Amz-SignedHeaders=host&X-Amz-Signature=0c5925dc554b2e175d58dcb96d9ad71cf85c81ba0835a523291f9f6ce90096ca");
    }

}
