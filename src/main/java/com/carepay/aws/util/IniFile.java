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
    private final File file;
    private final Map<String, Map<String, String>> multimap = new HashMap<>();

    public IniFile(final File file) {
        this.file = file;
        read();
    }

    protected void read() {
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), UTF_8)) {
            String line;
            String section = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[")) {
                    section = getSectionFromLine(line);
                } else {
                    addEntry(section, line);
                }
            }
        } catch (IOException e) { // NOSONAR
            // ignore
        }
    }

    private void addEntry(final String section, final String line) {
        int pos = line.indexOf('=');
        if (pos > 0) {
            final String key = line.substring(0, pos).trim();
            final String value = line.substring(pos + 1).trim();
            multimap.computeIfAbsent(section, k -> new HashMap<>()).put(key, value);
        }
    }

    private String getSectionFromLine(final String line) {
        return line.substring(line.startsWith("[profile ") ? 9 : 1, line.length() - 1);
    }

    public String getString(String section, String key) {
        return multimap.getOrDefault(section, Collections.emptyMap()).get(key);
    }

    public Map<String, String> getSection(String section) {
        return multimap.get(section);
    }
}
