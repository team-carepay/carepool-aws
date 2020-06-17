package com.carepay.aws.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ProcessCredentialsProvider implements CredentialsProvider {
    static final String CREDENTIAL_PROCESS = "credential_process";

    private final Map<String, String> section;

    public ProcessCredentialsProvider(final Map<String, String> section) {
        this.section = section;
    }

    @Override
    public Credentials getCredentials() {
        ProcessBuilder processBuilder = setupCommand();
        try {
            return executeCommand(processBuilder);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected ProcessBuilder setupCommand() {
        final String credentialsProcess = section.get(CREDENTIAL_PROCESS);
        List<String> cmd = new ArrayList<>();
        final String osName = System.getProperty("os.name");
        if (osName != null && osName.startsWith("Windows")) {
            cmd.add("cmd.exe");
            cmd.add("/C");
        } else {
            cmd.add("sh");
            cmd.add("-c");
        }
        cmd.add(credentialsProcess);
        return new ProcessBuilder(cmd);
    }

    protected Credentials executeCommand(ProcessBuilder processBuilder) throws IOException {
        Process process = processBuilder.start();
        try (final InputStream input = process.getInputStream();
             final InputStreamReader reader = new InputStreamReader(input, UTF_8)) {
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Command returned non-zero exit value: " + process.exitValue());
            }
            final JsonObject response = (JsonObject) Jsoner.deserialize(reader);
            return new Credentials(response.getString(CredentialsPropertyName.AccessKeyId),
                    response.getString(CredentialsPropertyName.SecretAccessKey),
                    response.getString(CredentialsPropertyName.SessionToken),
                    response.getString(CredentialsPropertyName.Expiration)
            );
        } catch (IOException | JsonException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            process.destroy();
        }
    }

    @SuppressWarnings("java:S115")
    private enum CredentialsPropertyName implements JsonKey {
        AccessKeyId,
        SecretAccessKey,
        SessionToken,
        Expiration,
        ;

        @Override
        public String getKey() {
            return name();
        }

        @Override
        public Object getValue() {
            return null;
        }
    }
}
