package com.carepay.aws;

import com.carepay.aws.ec2.EC2RegionProvider;

/**
 * Default RegionProvider which delegates to a chain of other providers: Environment,
 * SystemProperties, Profile config file or EC2 meta-data.
 */
public class DefaultRegionProviderChain implements RegionProvider {
    private final RegionProvider[] providers;

    public DefaultRegionProviderChain() {
        this(new EnvironmentRegionProvider(),
                new SystemPropertyRegionProvider(),
                new ProfileRegionProvider(),
                new EC2RegionProvider());
    }

    public DefaultRegionProviderChain(RegionProvider... providers) {
        this.providers = providers;
    }

    @Override
    public String getRegion() {
        for (RegionProvider p : providers) {
            final String region = p.getRegion();
            if (region != null) {
                return region;
            }
        }
        return null;
    }
}
