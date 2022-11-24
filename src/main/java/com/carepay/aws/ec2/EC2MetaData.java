package com.carepay.aws.ec2;

/**
 * Provides access to the EC2 metadata. This can be used to retrieve instance-id and region
 * information.
 */
public class EC2MetaData {
    private String instanceId;
    private String region;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
