package com.carepay.aws.ec2;

import java.io.IOException;

import com.carepay.aws.auth.RegionProvider;

/**
 * EC2 implementation of AWS region provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2RegionProvider implements RegionProvider {
    private final EC2 ec2;

    public EC2RegionProvider() {
        this(new EC2());
    }

    public EC2RegionProvider(final EC2 ec2) {
        this.ec2 = ec2;
    }

    @Override
    public String getRegion() {
        try {
            return ec2.queryMetaData().getRegion();
        } catch (IOException e) {
            return null;
        }
    }
}
