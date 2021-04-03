package com.carepay.aws.region;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentRegionProviderTest {

    @Test
    public void getDefaultRegion() {
        EnvironmentRegionProvider environmentRegionProvider = new EnvironmentRegionProvider(n -> "AWS_DEFAULT_REGION".equals(n) ? "kenya" : null);
        assertThat(environmentRegionProvider.getRegion()).isEqualTo("kenya");
    }

    @Test
    public void getRegion() {
        EnvironmentRegionProvider environmentRegionProvider = new EnvironmentRegionProvider(n -> "AWS_REGION".equals(n) ? "kenya" : null);
        assertThat(environmentRegionProvider.getRegion()).isEqualTo("kenya");
    }

    @Test
    public void getRegionNotConfigured() {
        EnvironmentRegionProvider environmentRegionProvider = new EnvironmentRegionProvider(n -> null);
        assertThat(environmentRegionProvider.getRegion()).isNull();
    }
}
