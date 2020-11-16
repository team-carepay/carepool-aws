package com.carepay.aws.auth;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;

import com.carepay.aws.util.Hex;
import com.carepay.aws.util.URLOpener;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SingleSignOnCredentialsProvider implements CredentialsProvider {

    private static final DateTimeFormatter EXPIRES_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendLiteral("UTC")
            .toFormatter()
            .withZone(ZoneId.of("UTC"));
    static final String SSO_START_URL = "sso_start_url";
    static final String SSO_REGION = "sso_region";
    static final String SSO_ACCOUNT_ID = "sso_account_id";
    static final String SSO_ROLE_NAME = "sso_role_name";

    final File cacheFile;
    final URL url;
    private final Clock clock;
    private final URLOpener opener;

    public SingleSignOnCredentialsProvider(final File awsDir, final Map<String, String> section, final Clock clock, final URLOpener opener) {
        final String ssoStartUrl = section.get(SSO_START_URL);
        final String region = section.get(SSO_REGION);
        final String accountId = section.get(SSO_ACCOUNT_ID);
        final String roleName = section.get(SSO_ROLE_NAME);
        try {
            final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            if (ssoStartUrl != null) {
                this.cacheFile = new File(awsDir, "sso/cache/" + Hex.encode(sha1.digest(ssoStartUrl.getBytes(UTF_8))) + ".json");
                this.url = URLOpener.create("https://portal.sso." + region + ".amazonaws.com/federation/credentials?role_name=" + roleName + "&account_id=" + accountId);
            } else {
                this.cacheFile = null;
                this.url = null;
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        this.clock = clock;
        this.opener = opener;
    }

    @Override
    public Credentials getCredentials() {
        if (cacheFile != null && cacheFile.exists()) {
            try {
                final JsonObject cache = (JsonObject) Jsoner.deserialize(new FileReader(cacheFile));
                final String accessToken = cache.getString(JsonProperty.accessToken);
                final String expiresAtString = cache.getString(JsonProperty.expiresAt);
                final Instant expiresDateTime = EXPIRES_FORMAT.parse(expiresAtString, Instant::from);
                if (expiresDateTime.isBefore(clock.instant())) {
                    return null;
                }
                final HttpURLConnection uc = this.opener.open(this.url);
                uc.setRequestProperty("x-amz-sso_bearer_token", accessToken);
                if (uc.getResponseCode() != 200) {
                    return null;
                }
                try (final InputStream in = uc.getInputStream();
                     final Reader reader = new InputStreamReader(in, UTF_8)) {
                    final JsonObject response = (JsonObject) Jsoner.deserialize(reader);
                    final JsonObject roleCredentials = response.getMap(JsonProperty.roleCredentials);
                    return new Credentials(
                            roleCredentials.getString(JsonProperty.accessKeyId),
                            roleCredentials.getString(JsonProperty.secretAccessKey),
                            roleCredentials.getString(JsonProperty.sessionToken),
                            Instant.ofEpochSecond(roleCredentials.getLong(JsonProperty.expiration) / 1000L, 0)
                    );
                }
            } catch (IOException | JsonException e) { // NOSONAR
                // ignored.
            }
        }
        return null;
    }

    @SuppressWarnings("java:S115")
    private enum JsonProperty implements JsonKey {
        accessToken,
        expiresAt,
        roleCredentials,
        accessKeyId,
        secretAccessKey,
        sessionToken,
        expiration,
        ;

        @Override
        public String getKey() {
            return name();
        }

        @Override
        public Object getValue() {
            return null;
        }
    }

}
