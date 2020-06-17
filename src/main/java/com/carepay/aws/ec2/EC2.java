package com.carepay.aws.ec2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.region.DefaultRegionProviderChain;
import com.carepay.aws.util.SimpleNamespaceContext;
import com.carepay.aws.util.URLOpener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides access to Amazon AWS API for Elastic Compute Cloud (EC2). Used to describe instances.
 */
public class EC2 {

    /**
     * parameters for describe tags command
     */
    private static final String DESCRIBE_TAGS_PARAMS = "Action=DescribeTags&Version=2016-11-15&Filter.1.Name=resource-id&Filter.1.Value.1=";
    /**
     * XML Namespace used by EC2
     */
    private static final String EC2_NAMESPACE = "http://ec2.amazonaws.com/doc/2016-11-15/";

    private final AWS4Signer signer;
    private final RegionProvider regionProvider;
    private final URLOpener opener;
    private final XPathExpression itemXpathExpression;
    private final XPathExpression keyXpathExpression;
    private final XPathExpression valueXpathExpression;

    public EC2() {
        this(new AWS4Signer(), DefaultRegionProviderChain.getInstance(), URLOpener.DEFAULT, XPathFactory.newInstance().newXPath());
    }

    public EC2(final AWS4Signer signer, final RegionProvider regionProvider, final URLOpener opener, final XPath xpath) {
        this.signer = signer;
        this.regionProvider = regionProvider;
        this.opener = opener;
        xpath.setNamespaceContext(new SimpleNamespaceContext("ec2", EC2_NAMESPACE));
        try {
            itemXpathExpression = xpath.compile("/ec2:DescribeTagsResponse/ec2:tagSet/ec2:item");
            keyXpathExpression = xpath.compile("ec2:key");
            valueXpathExpression = xpath.compile("ec2:value");
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> describeTags(final String instanceId) {
        try {
            final String payLoad = DESCRIBE_TAGS_PARAMS + instanceId;
            final URL url = new URL("https://ec2." + regionProvider.getRegion() + ".amazonaws.com");
            final HttpURLConnection uc = opener.open(url);
            uc.setRequestMethod("POST");
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            uc.setDoOutput(true);
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            signer.sign("ec2", uc, payLoad.getBytes(UTF_8));
            try (OutputStream outputSteam = uc.getOutputStream()) {
                outputSteam.write(payLoad.getBytes(UTF_8));
            }
            final int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream()) {
                if (responseCode >= 400) {
                    throw new IllegalArgumentException(uc.getResponseMessage());
                }
                final NodeList items = (NodeList) itemXpathExpression.evaluate(new InputSource(is), XPathConstants.NODESET);
                final Map<String, String> tags = new HashMap<>();
                for (int i = 0; i < items.getLength(); i++) {
                    final Element item = (Element) items.item(i);
                    final String key = keyXpathExpression.evaluate(item);
                    final String value = valueXpathExpression.evaluate(item);
                    tags.put(key, value);
                }
                return tags;
            } finally {
                uc.disconnect();
            }
        } catch (IOException | XPathExpressionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
