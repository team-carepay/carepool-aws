package com.carepay.aws.net;

import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

public class FormUrlEncodedUtils {
    public static String encode(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey()) + "=" + URLEncoder.encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }
}
