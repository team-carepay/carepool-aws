package com.carepay.aws.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class FirstLineResponseReader implements ResponseReader<String> {
    @Override
    public String read(HttpURLConnection uc) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(uc.getInputStream())))) {
            return br.readLine();
        }
    }
}
