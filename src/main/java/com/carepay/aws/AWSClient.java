package com.carepay.aws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.net.MockHttpURLConnection;
import com.carepay.aws.net.RequestWriter;
import com.carepay.aws.net.ResponseReader;
import com.carepay.aws.net.URLOpener;
import com.carepay.aws.net.WebClient;
import org.xml.sax.SAXException;

public class AWSClient extends WebClient {
    protected final AWS4Signer signer;

    protected static final SAXParser SAX_PARSER;

    static {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAX_PARSER = factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public AWSClient(final AWS4Signer signer, final URLOpener opener) {
        super(opener);
        this.signer = signer;
    }

    /**
     * Executes the HTTP request. Signs the headers, uploads the payload and extracts the response
     * information.
     *
     * @param url    the URL to visit
     * @param method HTTP method (e.g. GET, POST)
     * @param reader function to extract information, e.g. UploadId or ETag
     * @return the extracted information
     * @throws IOException in case of network issues
     */
    public <T> T signedExecute(final String method, final URL url, RequestWriter requestWriter, ResponseReader<T> reader, Map<String, String> headers) throws IOException {
        return super.execute(method, url, new SigningRequestWriter(requestWriter, signer), reader, headers);
    }

    /**
     * Helper method to retrieve the current region
     *
     * @return the AWS region
     */
    protected String getRegion() {
        return signer.getRegionProvider().getRegion();
    }

    static class SigningRequestWriter implements RequestWriter {
        private final RequestWriter requestWriter;
        private final AWS4Signer signer;

        public SigningRequestWriter(RequestWriter requestWriter, AWS4Signer signer) {
            this.requestWriter = requestWriter;
            this.signer = signer;
        }

        @Override
        public void write(HttpURLConnection uc) throws IOException {
            if (requestWriter != null) {
                final MockHttpURLConnection mockHttpURLConnection = new MockHttpURLConnection(uc.getURL());
                requestWriter.write(mockHttpURLConnection);
                final byte[] bytes = mockHttpURLConnection.getBytes();
                signer.signHeaders(uc, bytes);
                uc.getOutputStream().write(bytes);
            } else {
                signer.signHeaders(uc, null);
            }
        }
    }

}
