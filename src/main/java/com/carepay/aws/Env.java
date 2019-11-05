package com.carepay.aws;

public interface Env {
    String getEnv(String name);
    Env DEFAULT = System::getenv;
}
