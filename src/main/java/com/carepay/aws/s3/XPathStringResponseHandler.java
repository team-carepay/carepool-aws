package com.carepay.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.InputSource;

public class XPathStringResponseHandler extends XPathResponseHandler implements ResponseHandler<String> {
    public XPathStringResponseHandler(String expression) {
        super(expression);
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
}
