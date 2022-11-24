package com.carepay.aws.auth;

import java.time.Instant;
import java.util.Optional;

public class EC2Credentials extends Credentials {
    private String token;

    public EC2Credentials() {
        super(null, null, null, (Instant) null);
    }

    @Override
    public String getSessionToken() {
        return Optional.ofNullable(super.getSessionToken()).orElse(token);
    }
}
