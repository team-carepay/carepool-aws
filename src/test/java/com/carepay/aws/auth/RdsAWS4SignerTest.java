package com.carepay.aws.auth;

import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsAWS4SignerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));

    @Test
    public void testGeneratorSignature() throws MalformedURLException, GeneralSecurityException {
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "SeSsIoNtOkEn");
        RdsAWS4Signer signer = new RdsAWS4Signer(() -> credentials, () -> "eu-west-1", CLOCK);
        String token = signer.generateToken("dbhost.xyz.eu-west-1.amazonaws.com", 3306, "iam_user");
        assertThat(token).isEqualTo("dbhost.xyz.eu-west-1.amazonaws.com:3306/?Action=connect&DBUser=iam_user&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=SeSsIoNtOkEn&X-Amz-SignedHeaders=host&X-Amz-Signature=0c5925dc554b2e175d58dcb96d9ad71cf85c81ba0835a523291f9f6ce90096ca");
    }

    @Test
    public void testRealRdsToken() throws MalformedURLException, GeneralSecurityException {
        Clock clock = Clock.fixed(Instant.parse("2020-01-28T01:17:52.00Z"), ZoneId.of("UTC"));
        Credentials credentials = new Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        RdsAWS4Signer aws4signer = new RdsAWS4Signer(() -> credentials, () -> "eu-west-1", clock);

        String token = aws4signer.generateToken("rdsmysql.cdgmuqiadpid.us-west-2.rds.amazonaws.com", 3306, "jane_doe");
        assertThat(token).isEqualTo("rdsmysql.cdgmuqiadpid.us-west-2.rds.amazonaws.com:3306/?Action=connect&DBUser=jane_doe&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20200128%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20200128T011752Z&X-Amz-Expires=900&X-Amz-SignedHeaders=host&X-Amz-Signature=9fa655ff59886494ffce521cb732648b9dd8feeb71b9b14d3d1e2b8a3c500ce8");
    }


}
