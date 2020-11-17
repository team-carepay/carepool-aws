package com.carepay.aws.ec2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.carepay.aws.util.URLOpener;

/**
 * Provides access to the EC2 metadata. This can be used to retrieve instance-id and region
 * information.
 */
public class EC2MetaData {
    /**
     * EC2 metadata URL
     */
    @SuppressWarnings("java:S1313")
    public static final URL META_DATA_URL = createURL("http://169.254.169.254/latest/dynamic/instance-identity/document");

    private final ResourceFetcher resourceFetcher;

    public EC2MetaData() {
        this(new ResourceFetcher(new URLOpener.Default()));
    }

    public EC2MetaData(final ResourceFetcher resourceFetcher) {
        this.resourceFetcher = resourceFetcher;
    }

    static URL createURL(final String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String queryMetaDataAsString(final URL url) {
        return resourceFetcher.queryFirstLine(url);
    }

    public String getInstanceId() {
        return queryMetaData(META_DATA_URL).get("instanceId");
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> queryMetaData(final URL url) {
        return resourceFetcher.queryJson(url);
    }
}
