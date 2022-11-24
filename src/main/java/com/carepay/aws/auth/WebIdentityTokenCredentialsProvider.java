package com.carepay.aws.auth;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.carepay.aws.net.FormUrlEncodedRequestWriter;
import com.carepay.aws.net.URLOpener;
import com.carepay.aws.net.WebClient;
import com.carepay.aws.net.XmlResponseReader;
import com.carepay.aws.region.DefaultRegionProviderChain;
import com.carepay.aws.sts.AssumeRoleWithWebIdentityResponse;
import com.carepay.aws.util.Env;

public class WebIdentityTokenCredentialsProvider implements CredentialsProvider {
    private final String roleArn;
    private final File webIdentityTokenFile;
    private final WebClient webClient;
    private URL url;

    public WebIdentityTokenCredentialsProvider() {
        this(new DefaultRegionProviderChain(), new WebClient(new URLOpener.Default()), new Env.Default());
    }

    public WebIdentityTokenCredentialsProvider(final RegionProvider regionProvider, final WebClient webClient, final Env env) {
        this.webClient = webClient;
        this.roleArn = env.getEnv("AWS_ROLE_ARN");
        this.webIdentityTokenFile = Optional.ofNullable(env.getEnv("AWS_WEB_IDENTITY_TOKEN_FILE")).map(File::new).orElse(null);
        if ("regional".equals(env.getEnv("AWS_STS_REGIONAL_ENDPOINTS"))) {
            this.url = URLOpener.create("https://sts." + regionProvider.getRegion() + ".amazonaws.com");
        } else {
            this.url = URLOpener.create("https://sts.amazonaws.com");
        }
    }

    @Override
    public Credentials getCredentials() {
        if (webIdentityTokenFile == null) {
            return null;
        }
        try {
            final String token = readTokenFromFile();
            final Map<String, String> params = new HashMap<>();
            params.put("Action", "AssumeRoleWithWebIdentity");
            params.put("Version", "2011-06-15");
            params.put("RoleArn", roleArn);
            params.put("RoleSessionName", "aws-sdk-java-" + System.currentTimeMillis());
            params.put("WebIdentityToken", token);
            params.put("DurationSeconds", "3600");
            AssumeRoleWithWebIdentityResponse response = webClient.execute("POST", url, new FormUrlEncodedRequestWriter(params), new XmlResponseReader<>(AssumeRoleWithWebIdentityResponse.class), null);
            return response.getAssumeRoleWithWebIdentityResult().getCredentials();
        } catch (IOException e) {
            return null;
        }
    }

    protected String readTokenFromFile() throws IOException {
        return new String(Files.readAllBytes(webIdentityTokenFile.toPath()), StandardCharsets.UTF_8).trim();
    }

}
