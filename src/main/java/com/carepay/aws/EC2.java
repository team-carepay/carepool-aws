package com.carepay.aws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Provides access to Amazon AWS API for Elastic Compute Cloud (EC2)
 */
public class EC2 {

    private static final String DESCRIBE_TAGS_PARAMS = "Action=DescribeTags&Version=2016-11-15&Filter.1.Name=resource-id&Filter.1.Value.1=";
    private final AWS4Signer signer;
    private final URLOpener opener;
    private XPath xpath;

    public EC2() {
        this(new AWS4Signer(), URLOpener.DEFAULT);
    }

    public EC2(final AWS4Signer signer, final URLOpener opener) {
        this.signer = signer;
        this.opener = opener;
        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new SimpleNamespaceContext("http://ec2.amazonaws.com/doc/2016-11-15/", "ec2"));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> describeTags(final String region, final String instanceId) {
        try {
            String payLoad = DESCRIBE_TAGS_PARAMS + instanceId;
            URL url = new URL("https://ec2." + region + ".amazonaws.com/?");
            final HttpURLConnection urlConnection = opener.open(url);
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            signer.sign(urlConnection, payLoad);
            try (OutputStream outputSteam = urlConnection.getOutputStream()) {
                outputSteam.write(payLoad.getBytes());
            }
            try (final InputStream is = urlConnection.getInputStream()) {
                final NodeList items = (NodeList) xpath.evaluate("/ec2:DescribeTagsResponse/ec2:tagSet/ec2:item", new InputSource(is), XPathConstants.NODESET);
                final Map<String, String> tags = new HashMap<>();
                for (int i = 0; i < items.getLength(); i++) {
                    final Element item = (Element) items.item(i);
                    final String key = xpath.evaluate("ec2:key", item);
                    final String value = xpath.evaluate("ec2:value", item);
                    tags.put(key, value);
                }
                return tags;
            }
        } catch (IOException | XPathExpressionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
