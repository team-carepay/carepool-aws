package com.carepay.aws.auth;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.carepay.aws.util.URLOpener;
import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleSignOnCredentialsProviderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
    private SingleSignOnCredentialsProvider singleSignOnCredentialsProvider;
    private HttpURLConnection uc = mock(HttpURLConnection.class);
    private File awsDir;

    @Before
    public void setUp() throws URISyntaxException {
        awsDir = new File(getClass().getResource("/homedir/.aws").toURI());
    }

    @Test
    public void getCredentials() throws IOException {
        Map<String, String> section = new HashMap<>();
        section.put("sso_start_url", "https://carepay-sso.awsapps.com/start/");
        section.put("sso_account_id", "272942068046");
        section.put("sso_role_name", "AdministratorAccess");
        section.put("sso_region", "eu-west-1");
        when(uc.getResponseCode()).thenReturn(200);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/sso-role-credentials-response.json"));
        singleSignOnCredentialsProvider = new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc);
        final Credentials credentials = singleSignOnCredentialsProvider.getCredentials();
        assertThat(credentials.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentials.getExpiration()).isEqualTo(CLOCK.instant());
        assertThat(singleSignOnCredentialsProvider.url).isEqualTo(URLOpener.create("https://portal.sso.eu-west-1.amazonaws.com/federation/credentials?role_name=AdministratorAccess&account_id=272942068046"));
        verify(uc).setRequestProperty(eq("x-amz-sso_bearer_token"), eq("KilRoyWasHere"));
    }

    @Test
    public void emptyProfile() {
        Map<String, String> section = new HashMap<>();
        assertThat(new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc).getCredentials()).isNull();
        section.put("sso_start_url", "https://carepay-sso.awsapps.com/start/");
        assertThat(new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc).getCredentials()).isNull();
        section.put("sso_region", "eu-west-1");
        assertThat(new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc).getCredentials()).isNull();
        section.put("sso_account_id", "272942068046");
        assertThat(new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc).getCredentials()).isNull();
        section.put("sso_role_name", "AdministratorAccess");
        assertThat(new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc).getCredentials()).isNull();
    }

    @Test
    public void expiredCacheFile() {
        Map<String, String> section = new HashMap<>();
        section.put("sso_start_url", "https://acmecorp-sso.awsapps.com/start/");
        singleSignOnCredentialsProvider = new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc);
        assertThat(singleSignOnCredentialsProvider.cacheFile).hasName("9f3c18dac28e5730d862f149a4c3d0a6faa0b18d.json");
        assertThat(singleSignOnCredentialsProvider.getCredentials()).isNull();
    }

    @Test
    public void nonExistingCacheFile() {
        Map<String, String> section = new HashMap<>();
        section.put("sso_start_url", "https://doesnotexist-sso.awsapps.com/start/");
        singleSignOnCredentialsProvider = new SingleSignOnCredentialsProvider(awsDir, section, CLOCK, u -> uc);
        assertThat(singleSignOnCredentialsProvider.getCredentials()).isNull();
    }
}
