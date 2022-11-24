package com.carepay.aws.ec2;

import java.net.HttpURLConnection;

import com.carepay.aws.auth.AWS4Signer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2RegionProviderTest {

    private EC2RegionProvider provider;
    private HttpURLConnection uc;

    @BeforeEach
    public void setUp() throws Exception {
        uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/metadata.json"));
        EC2 ec2 = new EC2(mock(AWS4Signer.class), url -> uc);
        provider = new EC2RegionProvider(ec2);
    }

    @Test
    public void getRegion() {
        assertThat(provider.getRegion()).isEqualTo("ap-southeast-1");
    }
}
