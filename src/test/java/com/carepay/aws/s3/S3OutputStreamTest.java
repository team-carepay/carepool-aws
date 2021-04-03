package com.carepay.aws.s3;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3OutputStreamTest {
    private S3OutputStream victim;
    private S3 s3;

    @BeforeEach
    public void setUp() throws IOException {
        s3 = mock(S3.class);
        when(s3.startMultipart(anyString(), anyString())).thenReturn("123");
        victim = new S3OutputStream(s3, "testbucket", "testfolder/testpath.ext");
    }

    @Test
    public void testCancelShouldAbort() throws IOException {
        victim.write(new byte[S3OutputStream.BUFFER_SIZE]);
        victim.write('a');
        victim.cancel();
        verify(s3).abortMultipartUpload(anyString());
    }

    @Test
    public void testFailWhenWritingToClosedFile() throws IOException {
        victim.close();
        assertThatThrownBy(() -> victim.write('a')).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testCloseShouldCompleteMultipart() throws IOException {
        victim.write(new byte[S3OutputStream.BUFFER_SIZE + 1]);
        victim.close();
        verify(s3).finishMultipart(anyString());
    }


}
