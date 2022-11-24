package com.carepay.aws.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.carepay.aws.net.URLOpener;
import com.carepay.aws.net.WebClient;
import com.carepay.aws.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebIdentityTokenCredentialsProviderTest {

    private WebIdentityTokenCredentialsProvider provider;
    private Env env;

    @BeforeEach
    void setUp() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        env = mock(Env.class);
        when(env.getEnv("AWS_ROLE_ARN")).thenReturn("arn:aws:iam::578675666392:role/irsa-test-role");
        final URL tokenResource = getClass().getResource("/web-identity-token.txt");
        when(env.getEnv("AWS_WEB_IDENTITY_TOKEN_FILE")).thenReturn(tokenResource.getPath());
        provider = new WebIdentityTokenCredentialsProvider(() -> "eu-west-1", new WebClient(u -> uc), env);
        when(uc.getResponseCode()).thenReturn(200);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/sts-example.xml"));
        when(uc.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    }

    @Test
    void getCredentials() {
        Credentials credentials = provider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("AMYSECRETKEYIDNOBODY");
    }

    @Test
    void getCredentialsRegional() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        ArgumentCaptor<URL> c = ArgumentCaptor.forClass(URL.class);
        when(env.getEnv("AWS_STS_REGIONAL_ENDPOINTS")).thenReturn("regional");
        URLOpener opener = mock(URLOpener.class);
        when(opener.open(c.capture())).thenReturn(uc);
        provider = new WebIdentityTokenCredentialsProvider(() -> "us-east-1", new WebClient(opener), env);
        when(uc.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(uc.getResponseCode()).thenThrow(new IOException());
        Credentials credentials = provider.getCredentials();
        assertThat(credentials).isNull();
        assertThat(c.getValue()).isEqualTo(new URL("https://sts.us-east-1.amazonaws.com"));
    }
}