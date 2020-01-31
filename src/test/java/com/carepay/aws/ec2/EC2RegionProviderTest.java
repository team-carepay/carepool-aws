package com.carepay.aws.ec2;

import java.net.HttpURLConnection;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EC2RegionProviderTest {

    private EC2RegionProvider provider;
    private HttpURLConnection uc;

    @Before
    public void setUp() throws Exception {
        uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/metadata.json"));
        EC2MetaData ec2metadata = new EC2MetaData(url -> uc);
        provider = new EC2RegionProvider(ec2metadata);
    }

    @Test
    public void getRegion() {
        assertThat(provider.getRegion()).isEqualTo("ap-southeast-1");
    }
}
