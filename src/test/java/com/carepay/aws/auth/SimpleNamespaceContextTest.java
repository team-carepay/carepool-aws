package com.carepay.aws.auth;

import javax.xml.XMLConstants;

import com.carepay.aws.util.SimpleNamespaceContext;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleNamespaceContextTest {
    private SimpleNamespaceContext context;

    @Before
    public void setUp() {
        context = new SimpleNamespaceContext("ec2", "http://ec2.amazonaws.com/doc/2016-11-15/");
    }

    @Test
    public void getNamespaceURI() {
        assertThat(context.getNamespaceURI("ec2")).isEqualTo("http://ec2.amazonaws.com/doc/2016-11-15/");
        assertThat(context.getNamespaceURI("s3")).isEqualTo(XMLConstants.NULL_NS_URI);
        assertThat(context.getNamespaceURI(null)).isEqualTo(XMLConstants.NULL_NS_URI);
    }

    public void getPrefix() {
        assertThat(context.getPrefix("http://ec2.amazonaws.com/doc/2016-11-15/")).isEqualTo("ec2");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getPrefixes() {
        context.getPrefixes("http://ec2.amazonaws.com/doc/2016-11-15/");
    }
}
