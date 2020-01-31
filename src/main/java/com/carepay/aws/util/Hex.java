package com.carepay.aws.util;

public class Hex {
    private static final char[] HEX_DIGITS_LOWER = "0123456789abcdef".toCharArray();

    /**
     * HEX encode an array of bytes to lowercase hex string.
     *
     * @param bytes the byte array
     * @return a hex representation of the bytes
     */
    public static String encode(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS_LOWER[(b >> 4) & 0xf]).append(HEX_DIGITS_LOWER[b & 0xf]);
        }
        return sb.toString();
    }
}
