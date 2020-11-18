package com.carepay.aws.s3;

import java.net.HttpURLConnection;
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
    public void signHeaders(HttpURLConnection uc, byte[] payload, int offset, int length) {
        super.signHeaders(uc, null, 0, -1);
    }
}
