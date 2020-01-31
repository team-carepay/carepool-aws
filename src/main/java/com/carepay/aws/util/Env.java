package com.carepay.aws.util;

/**
 * Interface for accessing environment variables. This allows for mocking environment in
 * unit-tests.
 */
public interface Env {
    String getEnv(String name);

    Env DEFAULT = System::getenv;
}
