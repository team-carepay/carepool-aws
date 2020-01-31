package com.carepay.aws.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for reading ini files (e.g. ~/.aws/credentials and ~/.aws/config)
 */
public class IniFile {
    private File file;
    private Map<String, Map<String, String>> multimap = new HashMap<>();

    public IniFile(File file) throws IOException {
        this.file = file;
        read();
    }

    protected void read() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), UTF_8)) {
            String line;
            String section = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() >= 3 && !line.startsWith("#")) {
                    if (line.startsWith("[")) {
                        section = line.substring(1, line.length() - 1);
                    } else {
                        int pos = line.indexOf('=');
                        if (pos > 0) {
                            String key = line.substring(0, pos).trim();
                            String value = line.substring(pos + 1).trim();
                            multimap.computeIfAbsent(section, k -> new HashMap<>()).put(key, value);
                        }
                    }
                }
            }
        }
    }

    public String getString(String section, String key) {
        return multimap.getOrDefault(section, Collections.emptyMap()).get(key);
    }
}
