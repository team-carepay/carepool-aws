package com.carepay.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.carepay.aws.util.SimpleNamespaceContext;
import org.xml.sax.InputSource;

public class XPathResponseHandler implements ResponseHandler {
    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    static {
        XPATH.setNamespaceContext(new SimpleNamespaceContext("s3", "http://s3.amazonaws.com/doc/2006-03-01/"));

    }

    private final XPathExpression expression;

    public XPathResponseHandler(String expression) {
        try {
            this.expression = XPATH.compile(expression);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String extract(HttpURLConnection urlConnection) throws IOException {
        try {
            final InputStream inputStream = getInputStream(urlConnection);
            return expression.evaluate(new InputSource(inputStream));
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private InputStream getInputStream(HttpURLConnection urlConnection) throws IOException {
        return urlConnection.getResponseCode() < 400 ? urlConnection.getInputStream() : urlConnection.getErrorStream();
    }
}
