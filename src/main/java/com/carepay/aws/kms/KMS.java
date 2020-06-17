package com.carepay.aws.kms;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.util.URLOpener;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Lightweight implementation that provides access to AWS KMS services.
 */
public class KMS {
    private final AWS4Signer signer;
    private final URLOpener opener;
    private final URL url;

    public KMS() {
        this(new AWS4Signer(), URLOpener.DEFAULT);
    }

    /**
     * Creates a new KMS instance
     *
     * @param signer the AWS4 signer
     * @param opener the url opener
     */
    public KMS(final AWS4Signer signer, final URLOpener opener) {
        this.signer = signer;
        this.opener = opener;
        try {
            this.url = new URL("https://kms." + signer.getRegionProvider().getRegion() + ".amazonaws.com/");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Decrypts ciphertext back to plaintext
     *
     * @param cipherText the encrypted ciphertext
     * @return the decrypted plaintext
     */
    public String decrypt(final String cipherText) {
        final JsonObject request = new JsonObject()
                .putChain(KmsPropertyName.CiphertextBlob, cipherText);
        final JsonObject response = performKmsOperation(request, Target.Decrypt);
        return new String(Base64.getDecoder().decode(response.getString(KmsPropertyName.Plaintext)), UTF_8);
    }

    /**
     * Encrypts plaintext to ciphertext
     *
     * @param plaintext the plaintext string to encrypt
     * @param keyId     the AWS key-id to use
     * @return the encrypted ciphertext
     */
    public String encrypt(final String plaintext, final String keyId) {
        final JsonObject request = new JsonObject()
                .putChain(KmsPropertyName.Plaintext, plaintext)
                .putChain(KmsPropertyName.KeyId, keyId);
        final JsonObject response = performKmsOperation(request, Target.Encrypt);
        return response.getString(KmsPropertyName.CiphertextBlob);
    }

    /**
     * Executes a KMS operation.
     *
     * @param requestJson the request payload in JSON
     * @param target      specificies the target (Encrypt or Decrypt)
     * @return the response JSON
     */
    public JsonObject performKmsOperation(JsonObject requestJson, Target target) {
        try {
            final byte[] payLoad = requestJson.toJson().getBytes(UTF_8);
            final HttpURLConnection uc = opener.open(url);
            uc.setRequestMethod("POST");
            uc.setConnectTimeout(1000);
            uc.setReadTimeout(1000);
            uc.setDoOutput(true);
            uc.setRequestProperty("Content-Type", "application/x-amz-json-1.1");
            uc.setRequestProperty("X-Amz-Target", target.toString());
            signer.sign("kms", uc, payLoad);
            try (OutputStream outputSteam = uc.getOutputStream()) {
                outputSteam.write(payLoad);
            }
            int responseCode = uc.getResponseCode();
            try (final InputStream is = responseCode < 400 ? uc.getInputStream() : uc.getErrorStream();
                 final InputStreamReader reader = new InputStreamReader(is, UTF_8)) {
                //noinspection unchecked
                final JsonObject response = (JsonObject) Jsoner.deserialize(reader);
                if (responseCode >= 400) {
                    throw new IllegalArgumentException(response.getString(KmsPropertyName.message));
                }
                return response;
            } finally {
                uc.disconnect();
            }
        } catch (IOException | JsonException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Enum used for JSON request / response objects.
     */
    @SuppressWarnings("java:S115")
    private enum KmsPropertyName implements JsonKey {
        CiphertextBlob,
        KeyId,
        Plaintext,
        message;

        @Override
        public String getKey() {
            return name();
        }

        @Override
        public Object getValue() {
            return null;
        }
    }

    /**
     * Indicates the target operation
     */
    private enum Target {
        Encrypt,
        Decrypt;

        @Override
        public String toString() {
            return "TrentService." + name();
        }
    }
}
