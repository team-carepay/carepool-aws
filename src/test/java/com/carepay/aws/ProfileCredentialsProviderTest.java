package com.carepay.aws;

import java.io.File;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

public class ProfileCredentialsProviderTest {
    private static final File CREDENTIALS_FILE = new File(URI.create(ProfileCredentialsProviderTest.class.getResource("/homedir/.aws/credentials").toString()));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);
    private ProfileCredentialsProvider credentialsProvider;

    @Before
    public void setUp() {
        credentialsProvider = new ProfileCredentialsProvider(CREDENTIALS_FILE, CLOCK, n -> null);
    }

    @Test
    public void testGetCredentials() {
        Credentials creds = credentialsProvider.getCredentials();
        assertThat(creds).isNotNull();
        assertThat(creds.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }


    @Test
    public void testGetCredentialsAlternative() {
        String oldAwsProfile = System.getProperty("aws.profile");
        try {
            System.setProperty("aws.profile", "alternative");
            assertThat(new ProfileCredentialsProvider(CREDENTIALS_FILE, CLOCK, n -> null).getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE2");
        } finally {
            if (oldAwsProfile != null) {
                System.setProperty("aws.profile", oldAwsProfile);
            } else {
                System.clearProperty("aws.profile");
            }
        }
    }

    @Test
    public void testRefresh() {
        assertThat(credentialsProvider.getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
        assertThat(credentialsProvider.getCredentials().getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }

    @Test
    public void testProfileNotExists() {
        String oldUserHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "/not/existing/path");
            ProfileCredentialsProvider notExisting = new ProfileCredentialsProvider();
            assertThat(notExisting.getCredentials()).isNull();
        } finally {
            System.setProperty("user.home", oldUserHome);
        }
    }
}
