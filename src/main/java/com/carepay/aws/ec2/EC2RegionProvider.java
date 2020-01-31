package com.carepay.aws.ec2;

import com.carepay.aws.RegionProvider;

/**
 * EC2 implementation of AWS region provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2RegionProvider implements RegionProvider {
    private final EC2MetaData ec2metadata;
    private String lastRegion;

    public EC2RegionProvider() {
        this(new EC2MetaData());
    }

    public EC2RegionProvider(final EC2MetaData ec2metadata) {
        this.ec2metadata = ec2metadata;
    }

    @Override
    public String getRegion() {
        if (lastRegion == null) {
            lastRegion = ec2metadata.queryMetaData(EC2MetaData.META_DATA_URL).get("region");
        }
        return lastRegion;
    }
}
