package com.carepay.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.carepay.aws.util.SimpleNamespaceContext;

/**
 * Helper class to extract information from a HttpURLConnection response.
 */
public class XPathResponseHandler {
    protected final XPathExpression expression;

    public XPathResponseHandler(String expression) {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new SimpleNamespaceContext(
                "s3", "http://s3.amazonaws.com/doc/2006-03-01/",
                "ec2", "http://ec2.amazonaws.com/doc/2016-11-15/"
        ));
        try {
            this.expression = xpath.compile(expression);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    protected InputStream getInputStream(HttpURLConnection urlConnection) throws IOException {
        return urlConnection.getResponseCode() < 400 ? urlConnection.getInputStream() : urlConnection.getErrorStream();
    }
}
