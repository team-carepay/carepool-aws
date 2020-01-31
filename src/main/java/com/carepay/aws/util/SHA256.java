package com.carepay.aws.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.carepay.aws.util.Hex.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SHA256 {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String EMPTY_STRING_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    protected static MessageDigest getSha256MessageDigestInstance() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    protected static Mac getMacInstance() throws NoSuchAlgorithmException {
        return Mac.getInstance(HMAC_SHA_256);
    }

    /**
     * Applies Sha256 hashing on a string
     *
     * @param value the input string
     * @return the Sha256 digest in hex format
     */
    public static String hash(String value) {
        return hash(value.getBytes(UTF_8));
    }

    /**
     * Applies Sha256 hashing on a string
     *
     * @param value the input string
     * @return the Sha256 digest in hex format
     */
    public static String hash(final byte[] value) {
        if (value == null) {
            return EMPTY_STRING_SHA256;
        }
        return hash(value, 0, value.length);
    }

    /**
     * Applies Sha256 hashing on a string
     *
     * @param value the input string
     * @return the Sha256 digest in hex format
     */
    public static String hash(final byte[] value, final int offset, final int length) {
        if (value == null || length == 0) {
            return EMPTY_STRING_SHA256;
        }
        try {
            final MessageDigest sha256 = getSha256MessageDigestInstance();
            sha256.update(value, offset, length);
            return encode(sha256.digest());
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Calculates a signature for a String input
     *
     * @param value the String to sign
     * @param key   the key to use
     * @return the signature
     */
    public static byte[] sign(String value, byte[] key) {
        try {
            Mac mac = getMacInstance();
            mac.init(new SecretKeySpec(key, HMAC_SHA_256));
            return mac.doFinal(value.getBytes(UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
