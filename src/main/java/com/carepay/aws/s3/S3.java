package com.carepay.aws.s3;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.util.URLOpener;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * S3 client using lightweight API.
 */
public class S3 {

    private static final String AMAZONAWS_COM = ".amazonaws.com";
    private static final String HTTPS = "https";
    private static final String SERVICE_S3 = "s3";

    private static final ResponseHandler UPLOAD_ID_RESPONSE_HANDLER = new XPathResponseHandler("/s3:InitiateMultipartUploadResult/s3:UploadId");
    private static final ResponseHandler ERROR_MESSAGE_RESPONSE_HANDLER = new XPathResponseHandler("/Error/Message");
    private static final ResponseHandler ETAG_RESPONSE_HANDLER = uc -> uc.getHeaderField("ETag");
    private static final ResponseHandler NOOP_RESPONSE_HANDLER = uc -> null;

    private final AWS4Signer signer;
    private final URLOpener opener;
    private final Map<String, Multipart> multiparts = new HashMap<>();

    public S3() {
        this(new S3AWS4Signer(), new URLOpener.Default());
    }

    public S3(final AWS4Signer signer, final URLOpener opener) {
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
    protected String execute(final URL url, final String method, byte[] payload, int offset, int length, ResponseHandler responseHandler) throws IOException {
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
     * @return the extracted information
     * @throws IOException in case of network issues
     */
    protected void execute(HttpURLConnection uc, byte[] payload, int offset, int length) throws IOException {
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

    protected void assertResponseCode(HttpURLConnection uc) throws IOException {
        if (hasFailed(uc)) {
            throw new IOException(ERROR_MESSAGE_RESPONSE_HANDLER.extract(uc));
        }
    }

    private boolean hasFailed(HttpURLConnection uc) throws IOException {
        return uc.getResponseCode() >= 400;
    }

    /**
     * Initiates a new multipart upload.
     *
     * @param bucket the S3 bucket to upload to
     * @param path   the S3 key to upload to
     * @return the upload-id used for subsequent operations (UploadPart / FinishMultipart)
     * @throws IOException in case of error
     */
    public String startMultipart(String bucket, String path) throws IOException {
        final URL url = new URL(HTTPS, String.join(".", bucket, SERVICE_S3, getRegion(), AMAZONAWS_COM), path + "?uploads");
        final String uploadId = execute(url, "POST", null, 0, -1, UPLOAD_ID_RESPONSE_HANDLER);
        multiparts.put(uploadId, new Multipart(bucket, path));
        expireMultipartUploads();
        return uploadId;
    }

    /**
     * Helper method to retrieve the current region
     *
     * @return the AWS region
     */
    protected String getRegion() {
        return signer.getRegionProvider().getRegion();
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param uploadId the upload-id (returned from startMultipart)
     * @param buf      the byte-array to upload
     * @param offset   offset within the byte-array
     * @param length   the number of bytes to write
     */
    public void uploadPart(String uploadId, byte[] buf, int offset, int length) throws IOException {
        final Multipart multipart = getMultipart(uploadId);
        final int partNumber = multipart.parts.size() + 1;
        final URL url = new URL(HTTPS, multipart.bucket + ".s3." + getRegion() + AMAZONAWS_COM, multipart.key + "?PartNumber=" + partNumber + "&UploadId=" + uploadId);
        final String etag = execute(url, "PUT", buf, offset, length, ETAG_RESPONSE_HANDLER);
        multipart.parts.add(new PartETag(partNumber, etag));
    }

    protected Multipart getMultipart(String uploadId) {
        expireMultipartUploads();
        return (Multipart) Optional.ofNullable(multiparts.get(uploadId)).orElseThrow(() -> new IllegalArgumentException(uploadId));
    }

    /**
     * Completes a multipart upload.
     *
     * @param uploadId the upload-id
     * @throws IOException in case of error
     */
    public void finishMultipart(String uploadId) throws IOException {
        final Multipart multipart = getMultipart(uploadId);
        final URL url = new URL(HTTPS, String.join(".", multipart.bucket, SERVICE_S3, getRegion(), AMAZONAWS_COM), multipart.key + "?UploadId=" + uploadId);
        final StringBuilder sb = new StringBuilder("<CompleteMultipartUpload xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
        for (PartETag part : multipart.parts) {
            sb.append("<Part><ETag>").append(part.etag).append("</ETag><PartNumber>").append(part.partNumber).append("</PartNumber></Part>");
        }
        sb.append("</CompleteMultipartUpload>");
        final byte[] payload = sb.toString().getBytes(UTF_8);
        execute(url, "POST", payload, 0, payload.length, NOOP_RESPONSE_HANDLER);
        multiparts.remove(uploadId);
    }

    /**
     * Uploads a file to S3 in a single request.
     *
     * @param bucket the S3 bucket name
     * @param path   the S3 key
     * @param buf    the byte-array to upload
     * @param offset offset in the buffer
     * @param length number of bytes
     */
    public void putObject(String bucket, String path, byte[] buf, int offset, int length) throws IOException {
        final URL url = new URL(HTTPS, String.join(".", bucket, SERVICE_S3, getRegion(), AMAZONAWS_COM), path);
        execute(url, "PUT", buf, offset, length, NOOP_RESPONSE_HANDLER);
    }

    /**
     * Aborts multipart upload
     */
    public void abortMultipartUpload(String uploadId) throws IOException {
        final Multipart multipart = getMultipart(uploadId);
        final URL url = new URL(HTTPS, String.join(".", multipart.bucket, "s3", getRegion(), AMAZONAWS_COM), multipart.key + "?UploadId=" + uploadId);
        execute(url, "DELETE", null, 0, -1, NOOP_RESPONSE_HANDLER);
        multiparts.remove(uploadId);
    }

    protected void expireMultipartUploads() {
        long expirationTime = getExpirationTime(); // one hour
        multiparts.entrySet().removeIf(entry -> entry.getValue().timestamp < expirationTime);
    }

    private long getExpirationTime() {
        return System.nanoTime() - TimeUnit.NANOSECONDS.convert(1, TimeUnit.HOURS);
    }

    /**
     * Upload multipart in progress
     */
    private static class Multipart {
        private final String bucket;
        private final String key;
        private final List<PartETag> parts;
        private final long timestamp;

        private Multipart(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
            this.parts = new ArrayList<>();
            timestamp = System.nanoTime();
        }
    }

    /**
     * Wrapper object for ETags
     */
    static class PartETag {
        private final int partNumber;
        private final String etag;

        public PartETag(int partNumber, String etag) {
            this.partNumber = partNumber;
            this.etag = etag;
        }
    }
}
