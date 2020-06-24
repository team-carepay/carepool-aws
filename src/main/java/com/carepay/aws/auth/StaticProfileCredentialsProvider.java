package com.carepay.aws.auth;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.carepay.aws.util.IniFile;

public class StaticProfileCredentialsProvider implements CredentialsProvider {
    private static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    private static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    private static final String AWS_SESSION_TOKEN = "aws_session_token";
    private final File credentialsFile;
    private final String profileName;

    public StaticProfileCredentialsProvider(final File credentialsFile, final String profileName) {
        this.credentialsFile = credentialsFile;
        this.profileName = profileName;
    }

    @Override
    public Credentials getCredentials() {
        final IniFile iniFile = new IniFile(credentialsFile);
        final Map<String, String> section = Optional.ofNullable(
                iniFile.getSection(profileName))
                .orElseGet(
                        () -> Optional.ofNullable(
                                iniFile.getSection("default"))
                                .orElse(Collections.emptyMap())
                );

        final String accessKeyId = section.get(AWS_ACCESS_KEY_ID);
        final String secretAccessKey = section.get(AWS_SECRET_ACCESS_KEY);
        return accessKeyId != null && secretAccessKey != null ? new Credentials(
                accessKeyId,
                secretAccessKey,
                section.get(AWS_SESSION_TOKEN)
        ) : null;
    }
}
