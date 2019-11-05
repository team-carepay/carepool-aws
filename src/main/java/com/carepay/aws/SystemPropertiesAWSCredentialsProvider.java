package com.carepay.aws;

import java.util.Optional;

public class SystemPropertiesAWSCredentialsProvider implements AWSCredentialsProvider {
    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials(
                System.getProperty("aws.accessKeyId"),
                Optional.ofNullable(System.getProperty("aws.secretAccessKey")).orElseGet(() -> System.getProperty("aws.secretKey")),
                Optional.ofNullable(System.getProperty("aws.sessionToken")).orElseGet(() -> System.getProperty("aws.token"))
        );
    }
}
