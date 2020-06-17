package com.carepay.aws.region;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemPropertyRegionProviderTest {

    @Test
    public void getRegionMissing() {
        System.clearProperty("aws.region");
        SystemPropertyRegionProvider environmentRegionProvider = new SystemPropertyRegionProvider();
        assertThat(environmentRegionProvider.getRegion()).isNull();
    }

    @Test
    public void getRegion() {
        try {
            System.setProperty("aws.region", "kenya");
            SystemPropertyRegionProvider environmentRegionProvider = new SystemPropertyRegionProvider();
            assertThat(environmentRegionProvider.getRegion()).isEqualTo("kenya");
        } finally {
            System.clearProperty("aws.region");
        }
    }
}
