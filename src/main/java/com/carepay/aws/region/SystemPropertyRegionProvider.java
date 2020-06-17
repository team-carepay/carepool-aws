package com.carepay.aws.region;

import com.carepay.aws.auth.RegionProvider;

/**
 * Region provider which uses the Java System Propertiies (aws.region)
 */
public class SystemPropertyRegionProvider implements RegionProvider {
    @Override
    public String getRegion() {
        return System.getProperty("aws.region");
    }
}
