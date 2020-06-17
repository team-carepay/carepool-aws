package com.carepay.aws.auth;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import com.carepay.aws.region.DefaultRegionProviderChain;
import com.carepay.aws.util.Hex;
import com.carepay.aws.util.QueryStringUtils;
import com.carepay.aws.util.SHA256;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * Supports signing AWS requests. See https://docs.aws.amazon.com/general/latest/gr/signing_aws_api_requests.html
 */
public class AWS4Signer {
    /**
     * for performance reasons we cache the signing key, TTL is 24 hours
     */
    static final Map<String, byte[]> KEY_CACHE = new WeakHashMap<>();
    private static final String RDS_DB = "rds-db";
    private static final String AWS_4_REQUEST = "aws4_request";
    /**
     * the date-format used by AWS
     */
    private static final DateTimeFormatter AWS_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(UTC);
    private static final String AWS_4_HMAC_SHA_256 = "AWS4-HMAC-SHA256";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final String AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";

    protected final Clock clock;
    protected final CredentialsProvider credentialsProvider;
    protected final RegionProvider regionProvider;

    /**
     * Creates a new AWS4Signer
     */
    public AWS4Signer() {
        this(DefaultCredentialsProviderChain.getInstance(), DefaultRegionProviderChain.getInstance(), Clock.systemUTC());
    }

    /**
     * Creates a new AWS4Signer with a specific credentials-provider, region-provider and clock
     *
     * @param credentialsProvider AWS Credentials Provider
     * @param regionProvider      AWS Region Provider
     * @param clock               clock (configurable for tests)
     */
    public AWS4Signer(CredentialsProvider credentialsProvider, RegionProvider regionProvider, Clock clock) {
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.clock = clock;
    }


    /**
     * Adds AWS4 headers to the request.
     *
     * @param httpURLConnection the URL connection to sign
     * @param payload           the body of the request
     */
    public void sign(final String service, final HttpURLConnection httpURLConnection, final byte[] payload) {
        sign(service, httpURLConnection, payload, 0, payload != null ? payload.length : 0);
    }

    /**
     * Adds AWS4 headers to the request.
     *
     * @param uc      the URL connection to sign
     * @param payload the body of the request
     * @param offset  the offset in the payload
     * @param length  the length of the payload; use -1 to indicate the payload is unsigned
     */
    public void sign(final String service, final HttpURLConnection uc, final byte[] payload, final int offset, final int length) {
        new SignRequest(service, uc).signAuthorizationHeader(payload, offset, length);
    }

    /**
     * @param host   database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port   database port (MySQL uses 3306)
     * @param dbUser database username
     * @return the DB token
     */
    public String createDbAuthToken(final String host, final int port, final String dbUser) {
        return new SignRequest(host, port, dbUser).signQuery();
    }

    /**
     * Returns the signing key, or creates a new signing-key if required.
     *
     * @param credentials the AWS credentials to use
     * @param service     the name of the service
     * @param region      the AWS region (e.g. us-east-1)
     * @param dateStr     the date-string in YYYYMMDD format
     */
    protected byte[] getSigningKey(Credentials credentials, String service, String region, String dateStr) {
        final String cacheKey = service + region + credentials.getAccessKeyId() + dateStr;
        return KEY_CACHE.computeIfAbsent(cacheKey, key -> {
            final byte[] kSecret = ("AWS4" + credentials.getSecretAccessKey()).getBytes(UTF_8);
            final byte[] kDate = SHA256.sign(dateStr, kSecret);
            final byte[] kRegion = SHA256.sign(region, kDate);
            final byte[] kService = SHA256.sign(service, kRegion);
            return SHA256.sign(AWS_4_REQUEST, kService);
        });
    }

    public RegionProvider getRegionProvider() {
        return regionProvider;
    }

    /**
     * Wrapper class for HttpURLConnection, used for RDS-DB signing requests.
     */
    static class DBHttpURLConnection extends HttpURLConnection {

        public DBHttpURLConnection(final URL u) {
            super(u);
        }

        @Override
        public void disconnect() {
            // not needed for DB wrapper
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
            // not needed for DB wrapper
        }
    }

    /**
     * Helper class used for signing requests.
     */
    private class SignRequest {
        private final String service;
        private final URL url;
        private final HttpURLConnection urlConnection;
        private final TreeMap<String, String> signedHeaders;
        private final TreeMap<String, String> queryParams;
        private final String region;
        private final Credentials credentials;
        private final String dateTimeStr;
        private final String dateStr;
        private final String scope;

        public SignRequest(final String service, final HttpURLConnection urlConnection) {
            this.service = service;
            this.urlConnection = urlConnection;
            this.url = urlConnection.getURL();
            this.region = regionProvider.getRegion();
            this.credentials = credentialsProvider.getCredentials();
            this.dateTimeStr = AWS_DATE_FMT.format(clock.instant());
            this.dateStr = dateTimeStr.substring(0, 8);
            this.scope = dateStr + "/" + region + "/" + service + "/" + AWS_4_REQUEST;
            this.signedHeaders = new TreeMap<>(String::compareToIgnoreCase);
            this.queryParams = new TreeMap<>();
            signedHeaders.put("Host", url.getPort() > 0 ? url.getHost() + ":" + url.getPort() : url.getHost());
            queryParams.putAll(QueryStringUtils.parseQueryString(url.getQuery()));
        }

        /**
         * Creates a new SignRequest for RDS IAM authentication
         *
         * @param dbHost database hostname
         * @param dbPort database port
         * @param dbUser database user
         */
        public SignRequest(final String dbHost, final int dbPort, final String dbUser) {
            this(RDS_DB, new DBHttpURLConnection(createURL("https", dbHost, dbPort, "/?Action=connect&DBUser=" + dbUser)));
        }

        /**
         * Signs the request with AWS4 Authorization header.
         *
         * @param payload the request body
         * @param offset  offset in the payload
         * @param length  length of the payload
         */
        public void signAuthorizationHeader(final byte[] payload, final int offset, final int length) {
            signedHeaders.put("X-Amz-Date", dateTimeStr);
            final String contentSha256 = calculateContentSha256(payload, offset, length);
            final String stringToSign = getStringToSign(contentSha256);
            final byte[] signature = SHA256.sign(stringToSign, getSigningKey(credentials, service, region, dateStr));
            addAuthorizationHeader(signature);
        }

        private void addAuthorizationHeader(final byte[] signature) {
            urlConnection.setRequestProperty("Authorization", AWS_4_HMAC_SHA_256 +
                    " Credential=" + credentials.getAccessKeyId() + '/' + scope +
                    ", SignedHeaders=" + String.join(";", signedHeaders.keySet()).toLowerCase() +
                    ", Signature=" + Hex.encode(signature));
        }

        private String getStringToSign(final String contentSha256) {
            final StringBuilder canonicalStringBuilder = new StringBuilder(urlConnection.getRequestMethod()).append('\n')
                    .append(QueryStringUtils.uriEncodePath(url.getPath())).append('\n')
                    .append(QueryStringUtils.toQueryString(queryParams)).append('\n');
            signedHeaders.forEach((k, v) -> {
                canonicalStringBuilder.append(k.toLowerCase()).append(':').append(v).append('\n');
                urlConnection.setRequestProperty(k, v);
            });
            canonicalStringBuilder.append('\n')
                    .append(String.join(";", signedHeaders.keySet()).toLowerCase()).append('\n')
                    .append(contentSha256);
            return AWS_4_HMAC_SHA_256 + "\n" +
                    dateTimeStr + "\n" +
                    scope + "\n" +
                    SHA256.hash(canonicalStringBuilder.toString());
        }

        private String calculateContentSha256(final byte[] payload, final int offset, final int length) {
            if ("s3".equals(service)) {
                final String contentSha256 = length < 0 ? UNSIGNED_PAYLOAD : SHA256.hash(payload, offset, length);
                signedHeaders.put("X-Amz-Content-SHA256", contentSha256);
                if (this.credentials.hasToken()) {
                    signedHeaders.put(AMZ_SECURITY_TOKEN, this.credentials.getToken());
                }
                return contentSha256;
            } else {
                if (this.credentials.hasToken()) {
                    urlConnection.setRequestProperty(AMZ_SECURITY_TOKEN, this.credentials.getToken());
                }
                return SHA256.hash(payload, offset, length);
            }
        }

        /**
         * Signs the request using query string parameters
         *
         * @return the signed query
         */
        public String signQuery() {
            queryParams.put("X-Amz-Algorithm", AWS_4_HMAC_SHA_256);
            queryParams.put("X-Amz-Credential", credentials.getAccessKeyId() + "/" + scope);
            queryParams.put("X-Amz-Date", dateTimeStr);
            queryParams.put("X-Amz-Expires", "900");
            if (credentials.hasToken()) {
                queryParams.put(AMZ_SECURITY_TOKEN, credentials.getToken());
            }
            queryParams.put("X-Amz-SignedHeaders", String.join(";", signedHeaders.keySet()).toLowerCase());
            final String stringToSign = getStringToSign(SHA256.hash((byte[]) null));
            final byte[] signature = SHA256.sign(stringToSign, getSigningKey(credentials, service, region, dateStr));
            return url.getHost() + ":" + url.getPort() + url.getPath() + "?" + QueryStringUtils.toQueryString(queryParams) + "&X-Amz-Signature=" + Hex.encode(signature);
        }
    }

    static URL createURL(final String protocol, final String host, final int port, final String file) {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
