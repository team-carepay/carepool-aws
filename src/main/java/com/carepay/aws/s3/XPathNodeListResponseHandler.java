package com.carepay.aws.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XPathNodeListResponseHandler extends XPathResponseHandler implements ResponseHandler<NodeList> {
    public XPathNodeListResponseHandler(String expression) {
        super(expression);
    }

    @Override
    public NodeList extract(HttpURLConnection urlConnection) throws IOException {
        try {
            final InputStream inputStream = getInputStream(urlConnection);
            return (NodeList) expression.evaluate(new InputSource(inputStream), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }
}
