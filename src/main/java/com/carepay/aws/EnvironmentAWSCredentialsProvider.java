package com.carepay.aws;

import java.util.Optional;

/**
 * Environment implementation of AWS credentials providers. (AWS_ACCESS_KEY_ID /
 * AWS_SECRET_ACCESS_KEY / AWS_TOKEN)
 */
public class EnvironmentAWSCredentialsProvider implements AWSCredentialsProvider {
    private final Env env;

    public EnvironmentAWSCredentialsProvider() {
        this(Env.DEFAULT);
    }

    public EnvironmentAWSCredentialsProvider(Env env) {
        this.env = env;
    }

    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials(
                env.getEnv("AWS_ACCESS_KEY_ID"),
                Optional.ofNullable(env.getEnv("AWS_SECRET_ACCESS_KEY")).orElseGet(() -> env.getEnv("AWS_SECRET_KEY")),
                Optional.ofNullable(env.getEnv("AWS_SESSION_TOKEN")).orElseGet(() -> env.getEnv("AWS_TOKEN"))
        );
    }
}
