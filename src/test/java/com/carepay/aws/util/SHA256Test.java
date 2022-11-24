package com.carepay.aws.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SHA256Test {

    @Test
    public void getSha256MessageDigestInstance() throws NoSuchAlgorithmException {
        assertThat(SHA256.getSha256MessageDigestInstance()).isInstanceOf(MessageDigest.class);
    }

    @Test
    public void getMacInstance() throws NoSuchAlgorithmException {
        assertThat(SHA256.getMacInstance()).isInstanceOf(Mac.class);
    }

    @Test
    public void hashString() {
        assertThat(SHA256.hash("ABC")).isEqualTo("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78");
        assertThat(SHA256.hash((byte[]) null)).isEqualTo(SHA256.EMPTY_STRING_SHA256);
    }

    @Test
    public void hashByteArray() {
        assertThat(SHA256.hash(new byte[]{0x12, 0x34, 0x56, 0x78})).isEqualTo("b2ed992186a5cb19f6668aade821f502c1d00970dfd0e35128d51bac4649916c");
    }

    @Test
    public void hashByteArrayWithOfsLen() {
        assertThat(SHA256.hash(new byte[]{0x12, 0x34, 0x56, 0x78}, 2, 2)).isEqualTo("0bb404b255c8235d6d5113cc3e49262a99c374b16f16cd6bf2380052b091c876");
    }

    @Test
    public void sign() {
        byte[] result = new byte[]{33, -60, 14, -87, -27, 1, 89, -22, -59, -6, 118, -5, 91, -43, -55, 117, 66, -7, 7, 14, -72, -112, 42, 93, 39, -27, -127, -41, 25, 126, -4, 55};
        assertThat(SHA256.sign("FooBarWasHere", new byte[]{(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89})).isEqualTo(result);
    }
}
