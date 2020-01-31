package com.carepay.aws;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRegionProviderChainTest {

    @Test
    public void getRegion() {
        DefaultRegionProviderChain chain = new DefaultRegionProviderChain(() -> "us-east-1");
        assertThat(chain.getRegion()).isEqualTo("us-east-1");
    }

    @Test
    public void noRegionFound() {
        DefaultRegionProviderChain chain = new DefaultRegionProviderChain(() -> null);
        assertThat(chain.getRegion()).isNull();
    }
}
