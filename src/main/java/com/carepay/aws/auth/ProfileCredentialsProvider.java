package com.carepay.aws.auth;

import java.io.File;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.carepay.aws.util.Env;
import com.carepay.aws.util.IniFile;
import com.carepay.aws.util.URLOpener;

import static java.time.ZoneOffset.UTC;

/**
 * Credentials provider which supports the profile stored in the `.aws` folder in the homedir.
 */
public class ProfileCredentialsProvider implements CredentialsProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CONFIG_FILENAME = ".aws/config";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";

    private final CredentialsProvider delegateCredentialProvider;
    private final Clock clock;
    private Credentials lastCredentials;

    public ProfileCredentialsProvider() {
        this(new File(new File(System.getProperty(USER_HOME)), AWS_CONFIG_FILENAME), Env.DEFAULT, Clock.systemDefaultZone(), URLOpener.DEFAULT);
    }

    public ProfileCredentialsProvider(final File profileFile, final Env env, final Clock clock, final URLOpener opener) {
        final IniFile iniFile = new IniFile(profileFile);
        String profileName = Optional.ofNullable(env.getEnv(AWS_PROFILE))
                .orElseGet(() -> Optional.ofNullable(System.getProperty("aws.profile")).orElse(DEFAULT));
        final Map<String, String> section = Optional.ofNullable(
                iniFile.getSection(profileName))
                .orElseGet(
                        () -> Optional.ofNullable(
                                iniFile.getSection("default"))
                                .orElse(Collections.emptyMap())
                );
        if (section.containsKey(SingleSignOnCredentialsProvider.SSO_START_URL)) {
            delegateCredentialProvider = new SingleSignOnCredentialsProvider(profileFile.getParentFile(), section, clock, opener);
        } else if (section.containsKey(ProcessCredentialsProvider.CREDENTIAL_PROCESS)) {
            delegateCredentialProvider = new ProcessCredentialsProvider(section);
        } else {
            delegateCredentialProvider = new StaticProfileCredentialsProvider(new File(profileFile.getParentFile(), "credentials"), profileName);
        }
        this.clock = clock;
    }

    @Override
    public Credentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (lastCredentials == null || (lastCredentials.getExpiration() != null && lastCredentials.getExpiration().isBefore(now))) {
            lastCredentials = delegateCredentialProvider.getCredentials();
        }
        return lastCredentials;
    }
}
