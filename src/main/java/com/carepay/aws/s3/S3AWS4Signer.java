package com.carepay.aws.s3;

import java.time.Clock;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.auth.RegionProvider;

public class S3AWS4Signer extends AWS4Signer {
    public S3AWS4Signer() {
        super("s3");
    }

    public S3AWS4Signer(CredentialsProvider credentialsProvider, RegionProvider regionProvider, Clock clock) {
        super("s3", credentialsProvider, regionProvider, clock);
    }

    @Override
    protected String calculateContentSha256(byte[] payload, int offset, int length) {
        return UNSIGNED_PAYLOAD;
    }

    @Override
    public boolean isSecurityTokenSigned() {
        return true;
    }

    @Override
    public boolean isContentSha256Signed() {
        return true;
    }
}
