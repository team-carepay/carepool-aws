package com.carepay.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.util.SimpleNamespaceContext;
import com.carepay.aws.util.URLOpener;
import org.xml.sax.InputSource;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides access to Amazon AWS API for Elastic Compute Cloud (EC2). Used to describe instances.
 */
public class S3 {

    private static final String S3_NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01/";
    private static final int UNSIGNED_PAYLOAD = -1;
    private static final String UPLOAD_NOT_FOUND = "Upload not found: ";
    private static final String AMAZONAWS_COM = ".amazonaws.com";
    private static final String HTTPS = "https";
    private static final String S_3 = "s3";

    private final AWS4Signer signer;
    private final URLOpener opener;
    private final Map<String, Multipart> multiparts;
    private final XPathExpression uploadIdXpathExpression;
    private final XPathExpression errorMessageExpression;
    private RegionProvider regionProvider;

    public S3() {
        this(new AWS4Signer(), new URLOpener.Default(), XPathFactory.newInstance().newXPath());
    }

    public S3(final AWS4Signer signer, final URLOpener opener, final XPath xpath) {
        this.signer = signer;
        this.opener = opener;
        this.multiparts = new HashMap<>();
        this.regionProvider = signer.getRegionProvider();
        xpath.setNamespaceContext(new SimpleNamespaceContext(S_3, S3_NAMESPACE));
        try {
            uploadIdXpathExpression = xpath.compile("/s3:InitiateMultipartUploadResult/s3:UploadId");
            errorMessageExpression = xpath.compile("/Error/Message");
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
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
        final URL url = new URL(HTTPS, String.join(".", bucket, S_3, regionProvider.getRegion(), AMAZONAWS_COM), path + "?uploads");
        final HttpURLConnection uc = opener.open(url);
        try {
            uc.setRequestMethod("POST");
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            signer.sign(S_3, uc, null);
            final int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IOException(errorMessageExpression.evaluate(new InputSource(is)));
                }
                final String uploadId = uploadIdXpathExpression.evaluate(new InputSource(is));
                multiparts.put(uploadId, new Multipart(bucket, path));
                expireMultipartUploads();
                return uploadId;
            }
        } catch (XPathExpressionException e) {
            throw new IOException(uc.getResponseMessage() + e.getMessage(), e);
        } finally {
            uc.disconnect();
        }
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
        expireMultipartUploads();
        final Multipart multipart = Optional.ofNullable(multiparts.get(uploadId)).orElseThrow(() -> new IllegalArgumentException(UPLOAD_NOT_FOUND + uploadId));
        final int partNumber = multipart.parts.size() + 1;
        final URL url = new URL(HTTPS, multipart.bucket + ".s3." + regionProvider.getRegion() + AMAZONAWS_COM, multipart.key + "?PartNumber=" + partNumber + "&UploadId=" + uploadId);
        final HttpURLConnection uc = opener.open(url);
        try {
            uc.setRequestMethod("PUT");
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            uc.setDoOutput(true);
            signer.sign("s3", uc, null, 0, UNSIGNED_PAYLOAD);
            try (OutputStream outputSteam = uc.getOutputStream()) {
                outputSteam.write(buf, offset, length);
            }
            final int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IOException(errorMessageExpression.evaluate(new InputSource(is)));
                }
            } catch (XPathExpressionException e) {
                throw new IOException(e.getMessage(), e);
            }
            final PartETag eTag = new PartETag(partNumber, uc.getHeaderField("ETag"));
            multipart.parts.add(eTag);
        } finally {
            uc.disconnect();
        }
    }

    /**
     * Completes a multipart upload.
     *
     * @param uploadId the upload-id
     * @throws IOException in case of error
     */
    public void finishMultipart(String uploadId) throws IOException {
        expireMultipartUploads();
        final Multipart multipart = Optional.ofNullable(multiparts.get(uploadId)).orElseThrow(() -> new IllegalArgumentException(UPLOAD_NOT_FOUND + uploadId));
        final URL url = new URL(HTTPS, String.join(".", multipart.bucket, S_3, regionProvider.getRegion(), AMAZONAWS_COM), multipart.key + "?UploadId=" + uploadId);
        final HttpURLConnection urlConnection = opener.open(url);
        try {
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.setDoOutput(true);
            final StringBuilder sb = new StringBuilder("<CompleteMultipartUpload xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
            for (PartETag part : multipart.parts) {
                sb.append("<Part><ETag>").append(part.etag).append("</ETag><PartNumber>").append(part.partNumber).append("</PartNumber></Part>");
            }
            sb.append("</CompleteMultipartUpload>");
            final byte[] payload = sb.toString().getBytes(UTF_8);
            signer.sign("s3", urlConnection, payload);
            try (OutputStream outputSteam = urlConnection.getOutputStream()) {
                outputSteam.write(payload);
            }
            final int responseCode = urlConnection.getResponseCode();
            try (final InputStream is = responseCode < 400 ? urlConnection.getInputStream() : urlConnection.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IOException(errorMessageExpression.evaluate(new InputSource(is)));
                }
            } catch (XPathExpressionException e) {
                throw new IOException(e.getMessage(), e);
            }
            multiparts.remove(uploadId);
        } finally {
            urlConnection.disconnect();
        }
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
        final URL url = new URL(HTTPS, String.join(".", bucket, S_3, regionProvider.getRegion(), AMAZONAWS_COM), path);
        final HttpURLConnection uc = opener.open(url);
        try {
            uc.setRequestMethod("PUT");
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            uc.setDoOutput(true);
            signer.sign(S_3, uc, null, 0, UNSIGNED_PAYLOAD);
            try (OutputStream outputSteam = uc.getOutputStream()) {
                outputSteam.write(buf, offset, length);
            }
            final int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IOException(errorMessageExpression.evaluate(new InputSource(is)));
                }
            } catch (XPathExpressionException e) {
                throw new IOException(e.getMessage(), e);
            }
        } finally {
            uc.disconnect();
        }
    }

    /**
     * Aborts multipart upload
     */
    public void abortMultipartUpload(String uploadId) throws IOException {
        expireMultipartUploads();
        final Multipart multipart = Optional.ofNullable(multiparts.get(uploadId)).orElseThrow(() -> new IllegalArgumentException(UPLOAD_NOT_FOUND + uploadId));
        final URL url = new URL(HTTPS, multipart.bucket + ".s3." + regionProvider.getRegion() + AMAZONAWS_COM, multipart.key + "?UploadId=" + uploadId);
        final HttpURLConnection urlConnection = opener.open(url);
        try {
            urlConnection.setRequestMethod("DELETE");
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            signer.sign("s3", urlConnection, null);
            final int responseCode = urlConnection.getResponseCode();
            try (final InputStream is = responseCode < 400 ? urlConnection.getInputStream() : urlConnection.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IOException(errorMessageExpression.evaluate(new InputSource(is)));
                }
            } catch (XPathExpressionException e) {
                throw new IOException(e.getMessage(), e);
            }
            multiparts.remove(uploadId);
        } finally {
            urlConnection.disconnect();
        }
    }

    protected void expireMultipartUploads() {
        long expirationTime = System.nanoTime() - TimeUnit.NANOSECONDS.convert(1, TimeUnit.HOURS); // one hour
        multiparts.entrySet().removeIf(entry -> entry.getValue().timestamp < expirationTime);
    }

    /**
     * Upload multipart in progress
     */
    private static class Multipart {
        private final String bucket;
        private final String key;
        private final List<PartETag> parts;
        private long timestamp;

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
