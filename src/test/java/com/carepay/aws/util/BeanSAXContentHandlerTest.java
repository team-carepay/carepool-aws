package com.carepay.aws.util;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.carepay.aws.ec2.DescribeTagsResponse;
import com.carepay.aws.sts.AssumeRoleWithWebIdentityResponse;
import com.carepay.aws.sts.AssumeRoleWithWebIdentityResult;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;

class BeanSAXContentHandlerTest {

    @Test
    void testValidResponse() throws ParserConfigurationException, SAXException, IOException {
        AssumeRoleWithWebIdentityResponse response = new AssumeRoleWithWebIdentityResponse();
        BeanSAXContentHandler handler = new BeanSAXContentHandler(response);
        SAXParserFactory.newInstance().newSAXParser().parse(getClass().getResourceAsStream("/sts-example.xml"), handler);
        assertThat(response.getAssumeRoleWithWebIdentityResult()).isNotNull();
        AssumeRoleWithWebIdentityResult result = response.getAssumeRoleWithWebIdentityResult();
        assertThat(result.getCredentials().getAccessKeyId()).isEqualTo("AMYSECRETKEYIDNOBODY");
    }

    @Test
    void testEC2Payload() throws ParserConfigurationException, SAXException, IOException {
        DescribeTagsResponse response = new DescribeTagsResponse();
        BeanSAXContentHandler handler = new BeanSAXContentHandler(response);
        SAXParserFactory.newInstance().newSAXParser().parse(getClass().getResourceAsStream("/ec2-describe-tags-response.xml"), handler);
        assertThat(response.getTagSet()).isNotNull();
    }
}