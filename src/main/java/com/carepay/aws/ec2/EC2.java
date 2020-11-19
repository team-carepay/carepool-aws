package com.carepay.aws.ec2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.carepay.aws.AWSClient;
import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.s3.XPathNodeListResponseHandler;
import com.carepay.aws.s3.XPathStringResponseHandler;
import com.carepay.aws.util.URLOpener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static com.carepay.aws.util.DomUtils.getElement;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides access to Amazon AWS API for Elastic Compute Cloud (EC2). Used to describe instances.
 */
public class EC2 extends AWSClient {

    private static final XPathStringResponseHandler ERROR_MESSAGE_RESPONSE_HANDLER = new XPathStringResponseHandler("/Response/Errors/Error/Message");
    /**
     * parameters for describe tags command
     */
    private static final String DESCRIBE_TAGS_PARAMS = "Action=DescribeTags&Version=2016-11-15&Filter.1.Name=resource-id&Filter.1.Value.1=";

    private static final XPathNodeListResponseHandler ITEM_RESPONSE_HANDLER = new XPathNodeListResponseHandler("/ec2:DescribeTagsResponse/ec2:tagSet/ec2:item");

    public EC2() {
        this(new AWS4Signer("ec2"), new URLOpener.Default());
    }

    public EC2(final AWS4Signer signer, final URLOpener opener) {
        super(signer, opener);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> describeTags(final String instanceId) throws IOException {
        final byte[] payLoad = (DESCRIBE_TAGS_PARAMS + instanceId).getBytes(UTF_8);
        final URL url = new URL("https://ec2." + getRegion() + ".amazonaws.com");
        final NodeList items = execute(url, "POST", payLoad, 0, payLoad.length, ITEM_RESPONSE_HANDLER);
        final Map<String, String> tags = collectTags(items);
        return tags;
    }

    protected Map<String, String> collectTags(final NodeList items) {
        final Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < items.getLength(); i++) {
            final Element item = (Element) items.item(i);
            final String key = getElement(item, "key");
            final String value = getElement(item, "value");
            tags.put(key, value);
        }
        return tags;
    }

    @Override
    protected void handleFailedResponse(final HttpURLConnection uc) throws IOException {
        throw new IOException(ERROR_MESSAGE_RESPONSE_HANDLER.extract(uc));
    }
}
