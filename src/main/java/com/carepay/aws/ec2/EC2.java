package com.carepay.aws.ec2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.carepay.aws.AWSClient;
import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.net.FormUrlEncodedRequestWriter;
import com.carepay.aws.net.URLOpener;
import com.carepay.aws.net.XmlResponseReader;
import com.carepay.aws.util.JsonParser;

/**
 * Provides access to Amazon AWS API for Elastic Compute Cloud (EC2). Used to describe instances.
 */
public class EC2 extends AWSClient {

    @SuppressWarnings("java:S1313")
    public static final URL META_DATA_URL = URLOpener.create("http://169.254.169.254/latest/dynamic/instance-identity/document");

    private JsonParser parser = new JsonParser();

    public EC2() {
        this(new AWS4Signer("ec2"), new URLOpener.Default());
    }

    public EC2(final AWS4Signer signer, final URLOpener opener) {
        super(signer, opener);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> describeTags(final String instanceId) throws IOException {
        final Map<String, String> params = new HashMap<>();
        params.put("Action", "DescribeTags");
        params.put("Version", "2016-11-15");
        params.put("Filter.1.Name", "resource-id");
        params.put("Filter.1.Value.1", instanceId);
        final URL url = new URL("https://ec2." + getRegion() + ".amazonaws.com/");
        final DescribeTagsResponse response = super.signedExecute("POST", url, new FormUrlEncodedRequestWriter(params), new XmlResponseReader<>(DescribeTagsResponse.class), null);
        return response.getTagSet().stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    public EC2MetaData queryMetaData() throws IOException {
        return super.execute("GET", META_DATA_URL, null, uc -> parser.parse(uc.getInputStream(), EC2MetaData.class), null);
    }

    @Override
    protected void handleFailedResponse(final HttpURLConnection uc) throws IOException {
        final XmlResponseReader<ErrorResponse> reader = new XmlResponseReader<>(ErrorResponse.class);
        final ErrorResponse errorResponse = reader.read(uc);
        final String message = errorResponse.getErrors().stream().findFirst().map(Error::getMessage).orElse(uc.getResponseMessage());
        throw new IOException(message);
    }
}
