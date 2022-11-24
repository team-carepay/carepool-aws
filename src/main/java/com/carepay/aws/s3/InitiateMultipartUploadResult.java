package com.carepay.aws.s3;

public class InitiateMultipartUploadResult {
    private String bucket;
    private String key;
    private String uploadId;

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public String getUploadId() {
        return uploadId;
    }
}
