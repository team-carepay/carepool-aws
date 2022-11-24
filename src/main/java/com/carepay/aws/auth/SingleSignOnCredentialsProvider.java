package com.carepay.aws.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.carepay.aws.net.URLOpener;
import com.carepay.aws.util.Hex;
import com.carepay.aws.util.JsonParser;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SingleSignOnCredentialsProvider implements CredentialsProvider {

    static final String SSO_START_URL = "sso_start_url";
    static final String SSO_REGION = "sso_region";
    static final String SSO_ACCOUNT_ID = "sso_account_id";
    static final String SSO_ROLE_NAME = "sso_role_name";

    final File cacheFile;
    final URL url;
    private final Clock clock;
    private final URLOpener opener;

    private final JsonParser parser = new JsonParser();

    public SingleSignOnCredentialsProvider(final File awsDir, final Map<String, String> section, final Clock clock, final URLOpener opener) {
        final String ssoStartUrl = section.get(SSO_START_URL);
        final String region = section.get(SSO_REGION);
        final String accountId = section.get(SSO_ACCOUNT_ID);
        final String roleName = section.get(SSO_ROLE_NAME);
        try {
            @SuppressWarnings("java:S4790") final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
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
                final CachedCredentials cachedCredentials = parser.parse(new FileInputStream(cacheFile), CachedCredentials.class);
                if (cachedCredentials.expiresAt.isBefore(clock.instant())) {
                    return null; // SSO credentials are expired
                }
                final HttpURLConnection uc = this.opener.open(this.url);
                uc.setRequestProperty("x-amz-sso_bearer_token", cachedCredentials.accessToken);
                if (uc.getResponseCode() != 200) {
                    return null;
                }
                try (final InputStream in = uc.getInputStream();) {
                    final RoleCredentials roleCredentials = parser.parse(in, RoleCredentials.class);
                    return roleCredentials.roleCredentials;
                }
            } catch (IOException e) { // NOSONAR
                e.printStackTrace();
                // ignored.
            }
        }
        return null;
    }

    public static class CachedCredentials {
        private String accessToken;
        private Instant expiresAt;
    }

    public static class RoleCredentials {
        private Credentials roleCredentials;
    }
}
