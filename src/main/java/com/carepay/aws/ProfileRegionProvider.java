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
public class ProfileRegionProvider implements RegionProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CONFIG = ".aws/config";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";
    private static final String REGION = "region";

    protected final Clock clock;
    private final String profileName;
    protected final File profileFile;
    private String lastRegion;
    private LocalDateTime expirationTime;
    private long lastModified;

    public ProfileRegionProvider() {
        this(new File(new File(System.getProperty(USER_HOME)), AWS_CONFIG), Clock.systemUTC(), Env.DEFAULT);
    }

    public ProfileRegionProvider(final File profileFile, final Clock clock, final Env env) {
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
                lastRegion = iniFile.getString(profileName, REGION);
            } catch (IOException ignored) { // NOSONAR
            }
        }
    }

    @Override
    public String getRegion() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (now.isAfter(expirationTime)) {
            refresh();
            expirationTime = now.plus(5, ChronoUnit.MINUTES);
        }
        return lastRegion;
    }
}
