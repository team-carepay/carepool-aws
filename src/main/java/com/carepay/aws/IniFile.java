package com.carepay.aws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IniFile {
    private File file;
    private Map<String, Map<String, String>> multimap = new HashMap<>();

    public IniFile(File file) throws IOException {
        this.file = file;
        read();
    }

    protected void read() throws IOException {
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
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
