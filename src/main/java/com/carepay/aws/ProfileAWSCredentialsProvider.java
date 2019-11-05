package com.carepay.aws;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

/**
 * Credentials provider which supports the profile stored in the `.aws` folder in the homedir.
 */
public class ProfileAWSCredentialsProvider implements AWSCredentialsProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CREDENTIALS = ".aws/credentials";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";
    private static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    private static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    private static final String AWS_SESSION_TOKEN = "aws_session_token";

    protected final Clock clock;
    private final String profileName;
    private final File profileFile;
    private AWSCredentials lastCredentials = AWSCredentials.NULL;
    private LocalDateTime expirationTime;
    private long lastModified;

    public ProfileAWSCredentialsProvider() {
        this(Clock.systemUTC());
    }

    public ProfileAWSCredentialsProvider(Clock clock) {
        this.clock = clock;
        this.profileName = Optional.ofNullable(System.getenv(AWS_PROFILE))
                .orElseGet(() -> Optional.ofNullable(System.getProperty("aws.profile")).orElse(DEFAULT));
        File homeDir = new File(System.getProperty(USER_HOME));
        profileFile = new File(homeDir, AWS_CREDENTIALS);
        expirationTime = LocalDateTime.MIN;
    }

    /**
     * Refreshes the credentials
     */
    private synchronized void refresh() {
        if (profileFile.exists() && profileFile.lastModified() != lastModified) {
            try {
                lastModified = profileFile.lastModified();
                final IniFile iniFile = new IniFile(profileFile);
                lastCredentials = new AWSCredentials(
                        iniFile.getString(profileName, AWS_ACCESS_KEY_ID),
                        iniFile.getString(profileName, AWS_SECRET_ACCESS_KEY),
                        iniFile.getString(profileName, AWS_SESSION_TOKEN)
                );
            } catch (IOException ignored) { // NOSONAR
            }
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(),UTC);
        if (now.isAfter(expirationTime)) {
            refresh();
            expirationTime = now.plus(5, ChronoUnit.MINUTES);
        }
        return lastCredentials;
    }
}
