package com.carepay.aws;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.WeakHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * Supports signing AWS requests. See https://docs.aws.amazon.com/general/latest/gr/signing_aws_api_requests.html
 */
public class AWS4Signer {
    private static final char[] HEX_DIGITS_LOWER = "0123456789abcdef".toCharArray();
    private static final char[] HEX_DIGITS_UPPER = "0123456789ABCDEF".toCharArray();
    private static final String RDS_DB = "rds-db";
    private static final String AWS_4_REQUEST = "aws4_request";
    private static final String EMPTY_STRING_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /**
     * the date-format used by AWS
     */
    private static final DateTimeFormatter AWS_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(UTC);
    /**
     * for performance reasons we cache the signing key, TTL is 24 hours
     */
    private static final Map<String, SigningKey> keyCache = new WeakHashMap<>();
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String AWS_4_HMAC_SHA_256 = "AWS4-HMAC-SHA256";

    protected final Clock clock;
    protected final AWSCredentialsProvider credentialsProvider;

    public AWS4Signer() {
        this(Clock.systemUTC(),new DefaultAWSCredentialsProviderChain());
    }

    public AWS4Signer(Clock clock, AWSCredentialsProvider credentialsProvider) {
        this.clock = clock;
        this.credentialsProvider = credentialsProvider;
    }

    protected static MessageDigest getMessageDigestInstance() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    protected static Mac getMacInstance() throws NoSuchAlgorithmException {
        return Mac.getInstance(HMAC_SHA_256);
    }

    /**
     * HEX encode an array of bytes to lowercase hex string.
     *
     * @param bytes the byte array
     * @return a hex representation of the bytes
     */
    protected static String hex(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS_LOWER[(b >> 4) & 0xf]).append(HEX_DIGITS_LOWER[b & 0xf]);
        }
        return sb.toString();
    }

    /**
     * Applies Sha256 hashing on a string
     *
     * @param value the input string
     * @return the Sha256 digest in hex format
     */
    protected static String hash(String value) throws NoSuchAlgorithmException {
        return hex(getMessageDigestInstance().digest(value.getBytes(UTF_8)));
    }

    /**
     * Calculates a signature for a String input
     *
     * @param value the String to sign
     * @param key   the key to use
     * @return the signature
     */
    private static byte[] sign(String value, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = getMacInstance();
        mac.init(new SecretKeySpec(key, HMAC_SHA_256));
        return mac.doFinal(value.getBytes(UTF_8));
    }

    public static String uriEncode(CharSequence input) {
        StringBuilder result = new StringBuilder();
        final int len = input.length();
        for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
                result.append(ch);
            } else {
                result.append('%').append(HEX_DIGITS_UPPER[(ch >> 4) & 0xf]).append(HEX_DIGITS_UPPER[ch & 0xf]);
            }
        }
        return result.toString();
    }

    /**
     * Adds AWS4 headers to the request.
     *
     * @param httpURLConnection the URL connection to sign
     * @param payload       the body of the request
     */
    public void sign(final HttpURLConnection httpURLConnection, final String payload) {
        final AWSCredentials credentials = credentialsProvider.getCredentials();
        final URL url = httpURLConnection.getURL();
        final String host = url.getHost();
        final String[] parts = host.split("\\."); // e.g. ec2.eu-west-1.amazonaws.com
        final String service = parts[0]; // TODO: support mapping for some exotic services
        final String region = parts.length == 4 ? parts[1] : "us-east-1";
        final String dateTimeStr = AWS_DATE_FMT.format(clock.instant());
        final String dateStr = dateTimeStr.substring(0, 8);
        final String scope = String.join("/", dateStr, region, service, AWS_4_REQUEST);
        httpURLConnection.setRequestProperty("X-Amz-Date", dateTimeStr);
        try {
            final String canonicalRequestStr = String.join("\n",
                    httpURLConnection.getRequestMethod(),
                    url.getPath(),
                    url.getQuery(),
                    "host:" + host,
                    "x-amz-date:" + dateTimeStr,
                    "",
                    "host;x-amz-date",
                    hash(payload));
            final String stringToSign = String.join("\n",
                    AWS_4_HMAC_SHA_256,
                    dateTimeStr,
                    scope,
                    hash(canonicalRequestStr));
            final byte[] signingKeyBytes = getSigningKey(credentials, service, region, dateStr);
            final byte[] signature = sign(stringToSign, signingKeyBytes);
            httpURLConnection.setRequestProperty("Authorization", AWS_4_HMAC_SHA_256 + " Credential=" + credentials.getAccessKeyId() + "/" + scope + ", " + "SignedHeaders=host;x-amz-date, " + "Signature=" + hex(signature));
            if (credentials.getToken() != null) {
                httpURLConnection.setRequestProperty("X-Amz-Security-Token", credentials.getToken());
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * @param host        database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port        database port (MySQL uses 3306)
     * @param dbuser      database username
     * @return the DB token
     */
    public String createDbAuthToken(final String host, final int port, final String dbuser) {
        final AWSCredentials credentials = credentialsProvider.getCredentials();
        final String[] parts = host.split("\\."); // e.g. xxxx.yyyy.eu-west-1.rds.amazonaws.com
        final String region = parts[2]; // extract region from hostname
        final String dateTimeStr = AWS_DATE_FMT.format(clock.instant());
        final String dateStr = dateTimeStr.substring(0, 8);
        final String scope = String.join("/", dateStr, region, RDS_DB, AWS_4_REQUEST);
        final StringBuilder queryBuilder = new StringBuilder("Action=connect")
                .append("&DBUser=").append(dbuser)
                .append("&X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .append("&X-Amz-Credential=").append(uriEncode(credentials.getAccessKeyId()+"/"+scope))
                .append("&X-Amz-Date=").append(dateTimeStr)
                .append("&X-Amz-Expires=900");
        if (credentials.hasToken()) {
            queryBuilder.append("&X-Amz-Security-Token=").append(uriEncode(credentials.getToken()));
        }
        queryBuilder.append("&X-Amz-SignedHeaders=host");
        final String canonicalRequestStr = String.join("\n",
                "GET",
                "/", // path
                queryBuilder.toString(),
                "host:" + host + ":" + port,
                "", // indicates end of signed headers
                "host", // signed header names
                EMPTY_STRING_SHA256); // sha256 hash of empty string
        try {
            final String stringToSign = String.join("\n",
                    AWS_4_HMAC_SHA_256, // algorithm
                    dateTimeStr, // timestamp
                    scope,
                    hash(canonicalRequestStr)); // hash of the request
            final byte[] signingKeyBytes = getSigningKey(credentials, RDS_DB, region, dateStr);
            final byte[] signature = sign(stringToSign, signingKeyBytes);
            queryBuilder.append("&X-Amz-Signature=").append(hex(signature));
            return host + ":" + port + "/?" + queryBuilder.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the signing key, or creates a new signing-key if required.
     *
     * @param credentials the AWS credentials to use
     * @param service     the name of the service
     * @param region      the AWS region (e.g. us-east-1)
     * @param dateStr     the date-string in YYYYMMDD format
     */
    protected byte[] getSigningKey(AWSCredentials credentials, String service, String region, String dateStr) throws InvalidKeyException, NoSuchAlgorithmException {
        final String cacheKey = credentials.getAccessKeyId() + region;
        SigningKey signingKey = keyCache.get(cacheKey);
        if (signingKey == null || !signingKey.date.equals(dateStr)) {
            final byte[] kSecret = ("AWS4" + credentials.getSecretAccessKey()).getBytes(UTF_8);
            final byte[] kDate = sign(dateStr, kSecret);
            final byte[] kRegion = sign(region, kDate);
            final byte[] kService = sign(service, kRegion);
            final byte[] signingKeyBytes = sign(AWS_4_REQUEST, kService);
            signingKey = new SigningKey(dateStr, signingKeyBytes);
            keyCache.put(cacheKey, signingKey);
        }
        return signingKey.key;
    }

    /**
     * Utility class to cache signing keys
     */
    private static class SigningKey {
        private String date;
        private byte[] key;

        SigningKey(String date, byte[] key) {
            this.date = date;
            this.key = key;
        }
    }
}
