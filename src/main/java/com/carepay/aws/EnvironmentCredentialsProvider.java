package com.carepay.aws;

import java.util.Optional;

import com.carepay.aws.util.Env;

/**
 * Environment implementation of AWS credentials providers. (AWS_ACCESS_KEY_ID /
 * AWS_SECRET_ACCESS_KEY / AWS_TOKEN)
 */
public class EnvironmentCredentialsProvider implements CredentialsProvider {
    private final Env env;

    public EnvironmentCredentialsProvider() {
        this(Env.DEFAULT);
    }

    public EnvironmentCredentialsProvider(Env env) {
        this.env = env;
    }

    @Override
    public Credentials getCredentials() {
        return new Credentials(
                env.getEnv("AWS_ACCESS_KEY_ID"),
                Optional.ofNullable(env.getEnv("AWS_SECRET_ACCESS_KEY")).orElseGet(() -> env.getEnv("AWS_SECRET_KEY")),
                Optional.ofNullable(env.getEnv("AWS_SESSION_TOKEN")).orElseGet(() -> env.getEnv("AWS_TOKEN"))
        );
    }
}
