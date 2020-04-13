package com.carepay.aws;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProfileRegionProviderTest {
    private static final File PROFILE_FILE = new File(URI.create(ProfileRegionProviderTest.class.getResource("/homedir/.aws/config").toString()));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2015-08-30T12:36:00.00Z"), UTC);

    private ProfileRegionProvider provider;

    @Before
    public void setUp() throws URISyntaxException {
        this.provider = new ProfileRegionProvider(PROFILE_FILE, CLOCK, n -> null);
    }

    @Test
    public void getRegion() {
        assertThat(provider.getRegion()).isEqualTo("eu-west-1");
    }

    @Test
    public void userHomeTest() throws URISyntaxException {
        String oldUserHome = System.getProperty("user.home");
        try {
            File homeDir = new File(getClass().getResource("/homedir").toURI());
            System.setProperty("user.home", homeDir.getAbsolutePath());
            ProfileRegionProvider provider = new ProfileRegionProvider();
            assertThat(provider.profileFile).isEqualTo(new File(homeDir, ".aws/config"));
        } finally {
            System.setProperty("user.home", oldUserHome);
        }
    }


    @Test
    public void testRefresh() throws URISyntaxException {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(
                Instant.parse("2015-08-30T12:36:00.00Z"),
                Instant.parse("2016-08-30T12:36:00.00Z"),
                Instant.parse("2017-08-30T12:36:00.00Z"));
        File profileFile = spy(new File(getClass().getResource("/homedir/.aws/config").toURI()));
        when(profileFile.exists()).thenReturn(false, true);
        ProfileRegionProvider regionProvider = new ProfileRegionProvider(profileFile, clock, n -> null);
        assertThat(regionProvider.getRegion()).isNull();
        assertThat(regionProvider.getRegion()).isEqualTo("eu-west-1");
        assertThat(regionProvider.getRegion()).isEqualTo("eu-west-1");
    }

    @Test
    public void testFileModified() throws URISyntaxException {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(
                Instant.parse("2015-08-30T12:36:00.00Z"),
                Instant.parse("2016-08-30T12:36:00.00Z"),
                Instant.parse("2017-08-30T12:36:00.00Z"));
        File profileFile = spy(new File(getClass().getResource("/homedir/.aws/config").toURI()));
        when(profileFile.lastModified()).thenReturn(1L, 1L, 2L);
        ProfileRegionProvider provider = new ProfileRegionProvider(profileFile, clock, n -> null);
        provider.getRegion();
        provider.getRegion();
        provider.getRegion();
    }
}
