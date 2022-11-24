package com.carepay.aws.region;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.util.Env;
import com.carepay.aws.util.IniFile;

/*
 * Credentials provider which supports the profile stored in the `.aws` folder in the homedir.
 */
public class ProfileRegionProvider implements RegionProvider {
    private static final String USER_HOME = "user.home";
    private static final String AWS_CONFIG = ".aws/config";
    private static final String AWS_PROFILE = "AWS_PROFILE";
    private static final String DEFAULT = "default";
    private static final String REGION = "region";
    private final File file;
    private final Env env;

    public ProfileRegionProvider() {
        this(new File(new File(System.getProperty(USER_HOME)), AWS_CONFIG), new Env.Default());
    }

    public ProfileRegionProvider(final File file, final Env env) {
        this.file = file;
        this.env = env;
    }

    @Override
    public String getRegion() {
        final IniFile profileFile = new IniFile(file);
        final String profileName = Optional.ofNullable(env.getEnv(AWS_PROFILE))
                .orElseGet(() -> Optional.ofNullable(System.getProperty("aws.profile")).orElse(DEFAULT));
        final Map<String, String> section = Optional.ofNullable(
                profileFile.getSection(profileName)).orElseGet(() -> profileFile.getSection(DEFAULT));
        return section != null ? section.get(REGION) : null;
    }
}
