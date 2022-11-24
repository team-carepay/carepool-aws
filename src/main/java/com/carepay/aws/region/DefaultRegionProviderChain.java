package com.carepay.aws.region;

import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.ec2.EC2RegionProvider;

/**
 * Default RegionProvider which delegates to a chain of other providers: Environment,
 * SystemProperties, Profile config file or EC2 meta-data.
 */
public class DefaultRegionProviderChain implements RegionProvider {
    private static final RegionProvider INSTANCE = new DefaultRegionProviderChain();
    private final RegionProvider[] providers;
    private String region;

    public DefaultRegionProviderChain() {
        this(new EnvironmentRegionProvider(),
                new SystemPropertyRegionProvider(),
                new ProfileRegionProvider(),
                new EC2RegionProvider());
    }

    public DefaultRegionProviderChain(RegionProvider... providers) {
        this.providers = providers;
    }

    public static RegionProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public String getRegion() {
        if (region == null) {
            for (RegionProvider provider : providers) {
                region = provider.getRegion();
                if (region != null) {
                    break;
                }
            }
        }
        return region;
    }
}
