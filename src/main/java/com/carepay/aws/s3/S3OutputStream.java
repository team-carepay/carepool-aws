package com.carepay.aws.s3;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Streaming OutputStream for AWS S3. If the file-size fits the buffer, the upload will use a simple
 * PutObject operation. Otherwise, a multi-part upload is started. Closing the stream will finish
 * the multipart upload.
 */
public class S3OutputStream extends OutputStream {

    /**
     * Default chunk size is 16MB
     */
    public static final int BUFFER_SIZE = 16 * 1024 * 1024;

    /**
     * The bucket-name on Amazon S3
     */
    private final String bucket;

    /**
     * The path (key) name within the bucket
     */
    private final String path;

    /**
     * The temporary buffer used for storing the chunks
     */
    private final byte[] buf;

    /**
     * Amazon S3 client. Note: does not yet KMS
     */
    private final S3 s3;

    /**
     * The position in the buffer
     */
    private int position;

    /**
     * The unique id for this upload
     */
    private String uploadId;

    /**
     * indicates whether the stream is still open / valid
     */
    private boolean open;

    /**
     * Creates a new S3 OutputStream
     *
     * @param s3     the AmazonS3 client
     * @param bucket name of the bucket
     * @param path   path within the bucket
     */
    public S3OutputStream(S3 s3, String bucket, String path) {
        this.s3 = s3;
        this.bucket = bucket;
        this.path = path;
        this.buf = new byte[BUFFER_SIZE];
        this.position = 0;
        this.open = true;
    }

    /**
     * Write an array to the S3 output stream.
     *
     * @param b the byte-array to append
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes an array to the S3 Output Stream
     *
     * @param byteArray the array to write
     * @param offset    the offset into the array
     * @param length    the number of bytes to write
     */
    @Override
    public void write(final byte[] byteArray, final int offset, final int length) throws IOException {
        this.assertOpen();
        int ofs = offset;
        int len = length;
        int size;
        while (len > (size = this.buf.length - position)) {
            System.arraycopy(byteArray, ofs, this.buf, this.position, size);
            this.position += size;
            flushBufferAndRewind();
            ofs += size;
            len -= size;
        }
        System.arraycopy(byteArray, ofs, this.buf, this.position, len);
        this.position += len;
    }

    /**
     * Flushes the buffer by uploading a part to S3.
     */
    @Override
    public synchronized void flush() throws IOException {
        assertOpen();
        flushBufferAndRewind();
    }

    protected void flushBufferAndRewind() throws IOException {
        if (uploadId == null) {
            this.uploadId = s3.startMultipart(this.bucket, this.path);
        }
        uploadPart();
        this.position = 0;
    }

    protected void uploadPart() throws IOException {
        this.s3.uploadPart(uploadId, buf, 0, position);
    }

    @Override
    public void close() throws IOException {
        if (this.open) {
            this.open = false;
            if (this.uploadId != null) {
                if (this.position > 0) {
                    uploadPart();
                }
                this.s3.finishMultipart(uploadId);
            } else {
                this.s3.putObject(this.bucket, this.path, this.buf, 0, this.position);
            }
        }
    }

    public void cancel() throws IOException {
        this.open = false;
        if (this.uploadId != null) {
            this.s3.abortMultipartUpload(this.uploadId);
        }
    }

    @Override
    public void write(int b) throws IOException {
        this.assertOpen();
        if (position >= this.buf.length) {
            flushBufferAndRewind();
        }
        this.buf[position++] = (byte) b;
    }

    private void assertOpen() {
        if (!this.open) {
            throw new IllegalStateException("Closed");
        }
    }
}
