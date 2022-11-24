package com.carepay.aws.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultCredentialsProviderChainTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
    private Properties oldProperties;
    private DefaultCredentialsProviderChain credentialsProviderChain;

    @BeforeEach
    public void setUp() {
        oldProperties = System.getProperties();
        System.setProperty("aws.accessKeyId", "abc");
        System.setProperty("aws.secretAccessKey", "def");
        System.setProperty("aws.sessionToken", "ghi");
        credentialsProviderChain = new DefaultCredentialsProviderChain();
    }

    @AfterEach
    public void tearDown() {
        System.setProperties(oldProperties);
    }

    @Test
    public void getCredentials() {
        final Credentials credentials = credentialsProviderChain.getCredentials();
        final String accessKey = Optional.ofNullable(System.getenv("AWS_ACCESS_KEY_ID")).orElse("abc");
        assertThat(credentials.getAccessKeyId()).isEqualTo(accessKey);
    }

    @Test
    public void getCachedExpiringCredentials() {
        Clock brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        Credentials credentials1 = new Credentials("abc1", "def1", "ghi1", Instant.parse("2018-09-19T16:32:42.00Z"));
        CredentialsProvider cp = mock(CredentialsProvider.class);
        when(cp.getCredentials()).thenReturn(credentials1);
        DefaultCredentialsProviderChain chain = new DefaultCredentialsProviderChain(brokenClock, cp);
        assertThat(chain.getCredentials()).isEqualTo(credentials1);
        assertThat(chain.getCredentials()).isEqualTo(credentials1);
        verify(cp, times(1)).getCredentials();

        Credentials credentials2 = new Credentials("abc2", "def2", "ghi2", Instant.parse("2018-09-19T17:32:42.00Z"));
        when(cp.getCredentials()).thenReturn(credentials2);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T17:02:42.00Z"));
        assertThat(chain.getCredentials()).isEqualTo(credentials2);
        verify(cp, times(2)).getCredentials();
    }

    @Test
    public void getCachedNonExpiringCredentials() {
        Credentials nonExpiringCreds = new Credentials("abc1", "def1", null);
        CredentialsProvider cp = mock(CredentialsProvider.class);
        when(cp.getCredentials()).thenReturn(nonExpiringCreds);
        DefaultCredentialsProviderChain chain = new DefaultCredentialsProviderChain(CLOCK, cp);
        assertThat(chain.getCredentials()).isEqualTo(nonExpiringCreds);
        assertThat(chain.getCredentials()).isEqualTo(nonExpiringCreds);
        verify(cp, times(1)).getCredentials();
    }

    @Test
    public void cannotFindProviders() {
        DefaultCredentialsProviderChain chain = new DefaultCredentialsProviderChain(CLOCK, () -> null);
        assertThat(chain.getCredentials()).isNull();
    }
}
