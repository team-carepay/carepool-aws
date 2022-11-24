package com.carepay.aws.auth;

import java.io.File;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.carepay.aws.net.URLOpener;
import com.carepay.aws.util.Env;
import com.carepay.aws.util.IniFile;

/**
 * Credentials provider which supports the profile stored in the `.aws` folder in the homedir.
 */
public class ProfileCredentialsProvider implements CredentialsProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CONFIG_FILENAME = ".aws/config";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";

    private final File file;
    private final Env env;
    private final Clock clock;
    private final URLOpener opener;

    public ProfileCredentialsProvider() {
        this(new File(new File(System.getProperty(USER_HOME)), AWS_CONFIG_FILENAME), new Env.Default(), Clock.systemUTC(), new URLOpener.Default());
    }

    public ProfileCredentialsProvider(final File file, final Env env, final Clock clock, final URLOpener opener) {
        this.file = file;
        this.env = env;
        this.clock = clock;
        this.opener = opener;
    }

    @Override
    public Credentials getCredentials() {
        final String profileName = Optional.ofNullable(env.getEnv(AWS_PROFILE))
                .orElseGet(() -> Optional.ofNullable(System.getProperty("aws.profile")).orElse(DEFAULT));
        final Map<String, String> section = getIniFileSection(profileName);
        return getDelegateCredentialsProvider(profileName, section).getCredentials();
    }

    private Map<String, String> getIniFileSection(String profileName) {
        final IniFile iniFile = new IniFile(file);
        return Optional.ofNullable(
                        iniFile.getSection(profileName))
                .orElseGet(
                        () -> Optional.ofNullable(
                                        iniFile.getSection(DEFAULT))
                                .orElse(Collections.emptyMap())
                );
    }

    private CredentialsProvider getDelegateCredentialsProvider(String profileName, Map<String, String> section) {
        if (section.containsKey(SingleSignOnCredentialsProvider.SSO_START_URL)) {
            return new SingleSignOnCredentialsProvider(file.getParentFile(), section, clock, opener);
        } else if (section.containsKey(ProcessCredentialsProvider.CREDENTIAL_PROCESS)) {
            return new ProcessCredentialsProvider(section);
        } else {
            return new StaticProfileCredentialsProvider(new File(file.getParentFile(), "credentials"), profileName);
        }
    }
}
