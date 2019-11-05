package com.carepay.aws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileAWSCredentialsProviderTest {
    private String oldUserHome;
    private File tmpDir;
    private File awsDir;
    private File credentialsFile;
    private ProfileAWSCredentialsProvider credentialsProvider;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);

    @Before
    public void setUp() throws IOException {
        oldUserHome = System.getProperty("user.home");
        tmpDir = File.createTempFile("test", ".dir");
        saveDelete(tmpDir);
        awsDir = new File(tmpDir, ".aws");
        credentialsFile = new File(awsDir, "credentials");
        awsDir.mkdirs();
        saveCredentials("/credentials");
        System.setProperty("user.home", tmpDir.getAbsolutePath());
        credentialsProvider = new ProfileAWSCredentialsProvider(CLOCK);
    }

    private void saveCredentials(String resource) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(credentialsFile);
             InputStream is = getClass().getResourceAsStream(resource)) {
            final byte[] buf = new byte[512];
            int n;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        }
    }

    @After
    public void tearDown() {
        saveDelete(credentialsFile);
        saveDelete(awsDir);
        saveDelete(tmpDir);
    }

    private void saveDelete(File file) {
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    @Test
    public void testGetCredentials() {
        AWSCredentials creds = credentialsProvider.getCredentials();
        assertThat(creds).isNotNull();
        assertThat(creds.getAccessKeyId()).isEqualTo("AKIDEXAMPLE");
    }


    @Test
    public void testGetCredentialsAlternative() {
        try {
            System.setProperty("aws.profile", "alternative");
            credentialsProvider = new ProfileAWSCredentialsProvider(CLOCK);
            AWSCredentials creds = credentialsProvider.getCredentials();
            assertThat(creds).isNotNull();
            assertThat(creds.getAccessKeyId()).isEqualTo("AKIDEXAMPLE2");
        } finally {
            System.clearProperty("aws.profile");
        }
    }

    @Test
    public void testRefresh() throws IOException, InterruptedException {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(
                Instant.parse("2015-08-30T12:36:00.00Z"),
                Instant.parse("2016-08-30T12:36:00.00Z"));
        credentialsProvider = new ProfileAWSCredentialsProvider(clock);
        AWSCredentials credentials = credentialsProvider.getCredentials();
        Thread.sleep(1000L); // to ensure the last-modified timestamp is later
        saveCredentials("/credentials2");
        AWSCredentials credentials2 = credentialsProvider.getCredentials();
        assertThat(credentials2.getAccessKeyId()).isEqualTo("SECOND_CREDZ");
    }
}
