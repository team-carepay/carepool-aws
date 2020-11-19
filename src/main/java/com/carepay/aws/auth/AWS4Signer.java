package com.carepay.aws.auth;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String X_AMZ_DATE = "X-Amz-Date";
    private static final String X_AMZ_CONTENT_SHA_256 = "X-Amz-Content-SHA256";
    private static final String X_AMZ_ALGORITHM = "X-Amz-Algorithm";
    private static final String X_AMZ_CREDENTIAL = "X-Amz-Credential";
    private static final String X_AMZ_EXPIRES = "X-Amz-Expires";
    private static final String X_AMZ_SIGNED_HEADERS = "X-Amz-SignedHeaders";
    private static final String X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";
    private static final String AWS_4_REQUEST = "aws4_request";
    /**
     * the date-format used by AWS
     */
    private static final DateTimeFormatter AWS_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(UTC);
    private static final String AWS_4_HMAC_SHA_256 = "AWS4-HMAC-SHA256";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    protected final Clock clock;
    protected final CredentialsProvider credentialsProvider;
    protected final RegionProvider regionProvider;
    /**
     * for performance reasons we cache the signing key, TTL is 24 hours
     */
    private final Map<String, byte[]> keyCache = new WeakHashMap<>();
    private final String service;

    /**
     * Creates a new AWS4Signer
     */
    public AWS4Signer(final String service) {
        this(service, DefaultCredentialsProviderChain.getInstance(), DefaultRegionProviderChain.getInstance(), Clock.systemUTC());
    }

    /**
     * Creates a new AWS4Signer with a specific credentials-provider, region-provider and clock
     *
     * @param credentialsProvider AWS Credentials Provider
     * @param regionProvider      AWS Region Provider
     * @param clock               clock (configurable for tests)
     */
    public AWS4Signer(final String service, final CredentialsProvider credentialsProvider, final RegionProvider regionProvider, final Clock clock) {
        this.service = service;
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.clock = clock;
    }

    static URL createURL(final String protocol, final String host, final int port, final String file) {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected Consumer<String> getAddSecurityTokenFunction(final SignRequest signRequest) {
        return signRequest::addSecurityTokenToURLConnection;
    }

    /**
     * Adds AWS4 headers to the request.
     *
     * @param httpURLConnection the URL connection to sign
     * @param payload           the body of the request
     */
    public void signHeaders(final HttpURLConnection httpURLConnection, final byte[] payload) {
        signHeaders(httpURLConnection, payload, 0, payload != null ? payload.length : 0);
    }

    /**
     * Adds AWS4 headers to the request.
     *
     * @param uc      the URL connection to sign
     * @param payload the body of the request
     * @param offset  the offset in the payload
     * @param length  the length of the payload; use -1 to indicate the payload is unsigned
     */
    public void signHeaders(final HttpURLConnection uc, final byte[] payload, final int offset, final int length) {
        new SignRequest(uc).signHeaders(payload, offset, length);
    }

    public RegionProvider getRegionProvider() {
        return regionProvider;
    }

    /**
     * Helper class used for signing requests.
     */
    protected class SignRequest {
        private final URL url;
        private final HttpURLConnection urlConnection;
        private final TreeMap<String, String> signedHeaders;
        private final TreeMap<String, String> queryParams;
        private final String region;
        private final Credentials credentials;
        private final String dateTimeStr;
        private final String dateStr;
        private final String scope;

        public SignRequest(final HttpURLConnection urlConnection) {
            this.urlConnection = urlConnection;
            this.url = urlConnection.getURL();
            this.region = regionProvider.getRegion();
            this.credentials = credentialsProvider.getCredentials();
            this.dateTimeStr = AWS_DATE_FMT.format(clock.instant());
            this.dateStr = dateTimeStr.substring(0, 8);
            this.scope = String.join("/", dateStr, region, service, AWS_4_REQUEST);
            this.signedHeaders = new TreeMap<>(String::compareToIgnoreCase);
            this.queryParams = new TreeMap<>(QueryStringUtils.parseQueryString(url));
            signedHeaders.put("Host", QueryStringUtils.getHostname(url));
        }

        /**
         * Returns the signing key, or creates a new signing-key if required.
         */
        protected byte[] getSigningKey() {
            final String cacheKey = service + region + credentials.getAccessKeyId() + dateStr;
            return keyCache.computeIfAbsent(cacheKey, key -> createSigningKey());
        }

        protected byte[] createSigningKey() {
            final byte[] kSecret = getSecret();
            final byte[] kDate = SHA256.sign(dateStr, kSecret);
            final byte[] kRegion = SHA256.sign(region, kDate);
            final byte[] kService = SHA256.sign(service, kRegion);
            return SHA256.sign(AWS_4_REQUEST, kService);
        }

        private byte[] getSecret() {
            return ("AWS4" + credentials.getSecretAccessKey()).getBytes(UTF_8);
        }

        /**
         * Signs the request with AWS4 Authorization header.
         *
         * @param payload the request body
         * @param offset  offset in the payload
         * @param length  length of the payload
         */
        public void signHeaders(final byte[] payload, final int offset, final int length) {
            signedHeaders.put(X_AMZ_DATE, dateTimeStr);
            addOptionalSecurityToken();
            final String contentSha256 = length >= 0 ? SHA256.hash(payload, offset, length) : UNSIGNED_PAYLOAD;
            addOptionalContentSha256(contentSha256);
            final String stringToSign = getStringToSign(contentSha256);
            final byte[] signature = SHA256.sign(stringToSign, getSigningKey());
            addAuthorizationHeader(signature);
        }

        protected void addOptionalContentSha256(String contentSha256) {
            if ("s3".equals(service)) {
                signedHeaders.put(X_AMZ_CONTENT_SHA_256, contentSha256);
            }
        }

        protected void addOptionalSecurityToken() {
            final String securityToken = credentials.getToken();
            if (securityToken != null) {
                getAddSecurityTokenFunction(this).accept(securityToken);
            }
        }

        public void addSecurityTokenToSignedHeaders(final String securityToken) {
            signedHeaders.put(X_AMZ_SECURITY_TOKEN, securityToken);
        }

        public void addSecurityTokenToURLConnection(final String securityToken) {
            urlConnection.setRequestProperty(X_AMZ_SECURITY_TOKEN, securityToken);
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
            return String.join("\n", AWS_4_HMAC_SHA_256, dateTimeStr, scope, SHA256.hash(canonicalStringBuilder.toString()));
        }

        /**
         * Signs the request using query string parameters
         *
         * @return the signed query
         */
        public String signQuery() {
            queryParams.put(X_AMZ_ALGORITHM, AWS_4_HMAC_SHA_256);
            queryParams.put(X_AMZ_CREDENTIAL, credentials.getAccessKeyId() + "/" + scope);
            queryParams.put(X_AMZ_DATE, dateTimeStr);
            queryParams.put(X_AMZ_EXPIRES, "900");
            if (credentials.hasToken()) {
                queryParams.put(X_AMZ_SECURITY_TOKEN, credentials.getToken());
            }
            queryParams.put(X_AMZ_SIGNED_HEADERS, String.join(";", signedHeaders.keySet()).toLowerCase());
            final String stringToSign = getStringToSign(SHA256.EMPTY_STRING_SHA256);
            final byte[] signature = SHA256.sign(stringToSign, getSigningKey());
            return url.getHost() + ":" + url.getPort() + url.getPath() + "?" + QueryStringUtils.toQueryString(queryParams) + "&X-Amz-Signature=" + Hex.encode(signature);
        }
    }
}
