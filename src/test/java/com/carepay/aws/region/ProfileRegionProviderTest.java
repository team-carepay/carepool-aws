package com.carepay.aws.region;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class ProfileRegionProviderTest {
    private static final File PROFILE_FILE = new File(URI.create(ProfileRegionProviderTest.class.getResource("/homedir/.aws/config").toString()));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);

    private ProfileRegionProvider provider;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        this.provider = new ProfileRegionProvider(PROFILE_FILE, n -> null);
    }

    @Test
    public void getRegion() {
        assertThat(provider.getRegion()).isEqualTo("eu-west-1");
    }

    @Test
    public void userHomeTest() throws URISyntaxException {
        String oldUserHome = System.getProperty("user.home");
        String oldAwsProfile = System.getProperty("aws.profile");
        try {
            File homeDir = new File(getClass().getResource("/homedir").toURI());
            System.setProperty("user.home", homeDir.getAbsolutePath());
            ProfileRegionProvider provider = new ProfileRegionProvider();
            System.setProperty("aws.profile", "abcde");
            assertThat(new ProfileRegionProvider().getRegion()).isEqualTo("eu-west-1");
            System.setProperty("aws.profile", "kenya");
            assertThat(new ProfileRegionProvider(new File(homeDir, ".aws/config"), n -> null).getRegion()).isEqualTo("ap-southeast-1");
        } finally {
            System.setProperty("user.home", oldUserHome);
            if (oldAwsProfile != null) {
                System.setProperty("aws.profile", oldAwsProfile);
            } else {
                System.clearProperty("aws.profile");
            }
        }
    }


    @Test
    public void testNotExists() throws URISyntaxException {
        File profileFile = new File(new File(getClass().getResource("/homedir/.aws/").toURI()), "notexisting");
        ProfileRegionProvider regionProvider = new ProfileRegionProvider(profileFile, n -> null);
        assertThat(regionProvider.getRegion()).isNull();
    }

    @Test
    public void testAwsEnvironmentVar() throws URISyntaxException {
        File profileFile = spy(new File(getClass().getResource("/homedir/.aws/config").toURI()));
        ProfileRegionProvider provider = new ProfileRegionProvider(profileFile, e -> "kenya");
        assertThat(provider.getRegion()).isEqualTo("ap-southeast-1");
    }
}
