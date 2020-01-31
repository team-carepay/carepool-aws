package com.carepay.aws;

/**
 * Region provider which uses the Java System Propertiies (aws.region)
 */
public class SystemPropertyRegionProvider implements RegionProvider {
    @Override
    public String getRegion() {
        return System.getProperty("aws.region");
    }
}
