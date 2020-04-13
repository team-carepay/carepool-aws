package com.carepay.aws;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.carepay.aws.util.Env;
import com.carepay.aws.util.IniFile;

import static java.time.ZoneOffset.UTC;

/**
 * Credentials provider which supports the profile stored in the `.aws` folder in the homedir.
 */
public class ProfileCredentialsProvider implements CredentialsProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CREDENTIALS = ".aws/credentials";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";
    private static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    private static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    private static final String AWS_SESSION_TOKEN = "aws_session_token";

    protected final Clock clock;
    protected final File profileFile;
    private final String profileName;
    private Credentials lastCredentials;
    private LocalDateTime expirationTime;
    private long lastModified;

    public ProfileCredentialsProvider() {
        this(new File(new File(System.getProperty(USER_HOME)), AWS_CREDENTIALS), Clock.systemUTC(), Env.DEFAULT);
    }

    public ProfileCredentialsProvider(final File profileFile, final Clock clock, final Env env) {
        this.profileFile = profileFile;
        this.clock = clock;
        this.profileName = Optional.ofNullable(env.getEnv(AWS_PROFILE))
                .orElseGet(() -> Optional.ofNullable(System.getProperty("aws.profile")).orElse(DEFAULT));
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
                lastCredentials = new Credentials(
                        iniFile.getString(profileName, AWS_ACCESS_KEY_ID),
                        iniFile.getString(profileName, AWS_SECRET_ACCESS_KEY),
                        iniFile.getString(profileName, AWS_SESSION_TOKEN)
                );
            } catch (IOException ignored) { // NOSONAR
            }
        }
    }

    @Override
    public Credentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (now.isAfter(expirationTime)) {
            refresh();
            expirationTime = now.plus(5, ChronoUnit.MINUTES);
        }
        return lastCredentials;
    }
}
