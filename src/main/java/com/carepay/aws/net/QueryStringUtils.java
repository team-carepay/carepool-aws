package com.carepay.aws.net;

import java.net.URL;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class QueryStringUtils {
    private static final char[] HEX_DIGITS_UPPER = "0123456789ABCDEF".toCharArray();
    private static final char[] HEX_TO_CHAR = new char[]{
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,


            0x8, 0x9, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0x0
    };
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

    private QueryStringUtils() {
        throw new IllegalStateException();
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
     * Convert a Map to QueryString format
     *
     * @param map the map
     * @return the Query String (URL Encoded)
     */
    public static String toQueryPathString(final Map<String, String> map) {
        return map.entrySet()
                .stream()
                .map(e -> uriEncodePath(e.getKey()) + "=" + uriEncodePath(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Parse a query string to a Map
     *
     * @param url url including query-string
     * @return the URI Encoded string
     */
    public static Map<String, String> parseQueryString(final URL url) {
        final String queryString = url.getQuery();
        final Map<String, String> queryParams = new HashMap<>();
        if (isNotEmpty(queryString)) {
            for (String pair : queryString.split("&", -1)) {
                final int idx = pair.indexOf("=");
                queryParams.put(uriDecode(pair.substring(0, idx)), uriDecode(pair.substring(idx + 1)));
            }
        }
        return queryParams;
    }

    private static boolean isNotEmpty(String queryString) {
        return queryString != null && !queryString.isEmpty();
    }

    /**
     * Encodes a string to be URI encoded in UTF-8 encoding
     *
     * @param input the input string
     * @return the encoded string
     */
    public static String uriEncode(String input) {
        final StringBuilder result = new StringBuilder();
        for (byte singleByte : input.getBytes(UTF_8)) {
            final char ch = (char) singleByte;
            if (ENCODING_NOT_NEEDED.get(ch)) {
                result.append(ch);
            } else {
                appendEncoded(result, ch);
            }
        }
        return result.toString();
    }

    private static void appendEncoded(StringBuilder result, char ch) {
        result.append('%').append(HEX_DIGITS_UPPER[(ch >> 4) & 0xf]).append(HEX_DIGITS_UPPER[ch & 0xf]);
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
            if (ENCODING_NOT_NEEDED_PATH.get(ch)) {
                result.append(ch);
            } else {
                appendEncoded(result, ch);
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
        final byte[] bytes = new byte[input.length()];
        final PrimitiveIterator.OfInt iter = input.chars().iterator();
        int j = 0;
        int ch;
        while (iter.hasNext()) {
            if ((ch = iter.nextInt()) == '%') {
                ch = uriDecodeChar(iter);
            }
            bytes[j++] = (byte) ch;
        }
        return new String(bytes, 0, j, UTF_8);
    }

    protected static char uriDecodeChar(final PrimitiveIterator.OfInt iter) {
        return (char) (HEX_TO_CHAR[iter.next()] << 4 | HEX_TO_CHAR[iter.next()]);
    }

    public static String getHostname(final URL url) {
        if (hasCustomPort(url)) {
            return url.getHost() + ":" + url.getPort();
        } else {
            return url.getHost();
        }
    }

    public static boolean hasCustomPort(final URL url) {
        return url.getPort() > 0;
    }
}
