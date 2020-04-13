package com.carepay.aws;

import com.carepay.aws.util.Env;

/**
 * RegionProvider which uses AWS_REGION environment variable.
 */
public class EnvironmentRegionProvider implements RegionProvider {
    private final Env env;

    public EnvironmentRegionProvider() {
        this(Env.DEFAULT);
    }

    public EnvironmentRegionProvider(Env env) {
        this.env = env;
    }

    @Override
    public String getRegion() {
        final String region = env.getEnv("AWS_DEFAULT_REGION");
        return region != null ? region : env.getEnv("AWS_REGION");
    }
}
