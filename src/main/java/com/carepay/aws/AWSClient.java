package com.carepay.aws;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.s3.ResponseHandler;
import com.carepay.aws.util.URLOpener;

public class AWSClient {
    protected final AWS4Signer signer;
    protected final URLOpener opener;

    public AWSClient(final AWS4Signer signer, final URLOpener opener) {
        this.signer = signer;
        this.opener = opener;
    }

    /**
     * Executes the HTTP request. Signs the headers, uploads the payload and extracts the response
     * information.
     *
     * @param url             the URL to visit
     * @param method          HTTP method (e.g. GET, POST)
     * @param payload         the request payload
     * @param offset          offset in the payload
     * @param length          length of the payload
     * @param responseHandler function to extract information, e.g. UploadId or ETag
     * @return the extracted information
     * @throws IOException in case of network issues
     */
    protected <T> T execute(final URL url, final String method, byte[] payload, int offset, int length, ResponseHandler<T> responseHandler) throws IOException {
        final HttpURLConnection uc = opener.open(url);
        try {
            uc.setRequestMethod(method);
            execute(uc, payload, offset, length);
            return responseHandler.extract(uc);
        } finally {
            uc.disconnect();
        }
    }

    /**
     * Executes the HTTP request. Signs the headers and uploads the payload.
     *
     * @param uc      the HTTP connection
     * @param payload the request payload
     * @param offset  offset in the payload
     * @param length  length of the payload
     * @throws IOException in case of network issues
     */
    protected void execute(final HttpURLConnection uc, final byte[] payload, final int offset, final int length) throws IOException {
        uc.setConnectTimeout(1000);
        uc.setReadTimeout(1000);
        signer.signHeaders(uc, payload, offset, length);
        if (length > 0) {
            try (OutputStream outputSteam = uc.getOutputStream()) {
                outputSteam.write(payload, offset, length);
            }
        }
        assertResponseCode(uc);
    }

    protected void assertResponseCode(final HttpURLConnection uc) throws IOException {
        if (hasFailed(uc)) {
            handleFailedResponse(uc);
        }
    }

    protected void handleFailedResponse(final HttpURLConnection uc) throws IOException {
        throw new IOException(uc.getResponseMessage());
    }

    protected boolean hasFailed(final HttpURLConnection uc) throws IOException {
        return uc.getResponseCode() >= 400;
    }

    /**
     * Helper method to retrieve the current region
     *
     * @return the AWS region
     */
    protected String getRegion() {
        return signer.getRegionProvider().getRegion();
    }
}
