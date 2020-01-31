package com.carepay.aws.util;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class QueryStringUtils {
    private static final char[] HEX_DIGITS_UPPER = "0123456789ABCDEF".toCharArray();
    private static final BitSet ENCODING_NOT_NEEDED = new BitSet(256);
    private static final BitSet ENCODING_NOT_NEEDED_PATH = new BitSet(256);

    static {
        ENCODING_NOT_NEEDED.set('a', 'z' + 1);
        ENCODING_NOT_NEEDED.set('A', 'Z' + 1);
        ENCODING_NOT_NEEDED.set('0', '9' + 1);
        ENCODING_NOT_NEEDED.set('~');
        ENCODING_NOT_NEEDED.set('-');
        ENCODING_NOT_NEEDED.set('_');
        ENCODING_NOT_NEEDED.set('.');
        ENCODING_NOT_NEEDED_PATH.or(ENCODING_NOT_NEEDED);
        ENCODING_NOT_NEEDED_PATH.set('/');
    }

    /**
     * Convert a Map to QueryString format
     *
     * @param map the map
     * @return the Query String (URL Encoded)
     */
    public static String toQueryString(final Map<String, String> map) {
        return map.entrySet()
                .stream()
                .map(e -> uriEncode(e.getKey()) + "=" + uriEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Parse a query string to a Map
     *
     * @param queryString the URI encoded string (name=value&name2=value2)
     * @return the URI Encoded string
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> queryParams = new HashMap<>();
        if (queryString != null && queryString.length() > 0) {
            for (String pair : queryString.split("&", -1)) {
                int idx = pair.indexOf("=");
                queryParams.put(uriDecode(pair.substring(0, idx)), uriDecode(pair.substring(idx + 1)));
            }
        }
        return queryParams;
    }

    /**
     * Encodes a string to be URI encoded in UTF-8 encoding
     *
     * @param input the input string
     * @return the encoded string
     */
    public static String uriEncode(String input) {
        final StringBuilder result = new StringBuilder();
        final byte[] bytes = input.getBytes(UTF_8);
        for (byte singleByte : bytes) {
            final char ch = (char) singleByte;
            if (ch < 128 && ENCODING_NOT_NEEDED.get(ch)) {
                result.append(ch);
            } else {
                result.append('%').append(HEX_DIGITS_UPPER[(ch >> 4) & 0xf]).append(HEX_DIGITS_UPPER[ch & 0xf]);
            }
        }
        return result.toString();
    }

    /**
     * Encodes a string to be URI encoded in UTF-8 encoding. Slash '/' is not encoded.
     *
     * @param input the input string
     * @return the encoded string
     */
    public static String uriEncodePath(final String input) {
        final StringBuilder result = new StringBuilder();
        final byte[] bytes = input.getBytes(UTF_8);
        for (byte singleByte : bytes) {
            final char ch = (char) singleByte;
            if (ch < 128 && ENCODING_NOT_NEEDED_PATH.get(ch)) {
                result.append(ch);
            } else {
                result.append('%').append(HEX_DIGITS_UPPER[(ch >> 4) & 0xf]).append(HEX_DIGITS_UPPER[ch & 0xf]);
            }
        }
        return result.toString();
    }

    /**
     * Decodes an encoded String back to a regular String
     *
     * @param input the URI Encoded String
     */
    public static String uriDecode(final String input) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final int len = input.length();
        for (int i = 0; i < len; ) {
            final char ch = input.charAt(i++);
            if (ch == '%') {
                result.write((char) Integer.parseInt(input.substring(i, i + 2), 16));
                i += 2;
            } else {
                result.write(ch);
            }
        }
        return new String(result.toByteArray(), UTF_8);
    }
}
