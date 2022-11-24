package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.carepay.aws.util.JsonParser;

public class JsonResponseReader<T> implements ResponseReader<T> {
    private final JsonParser parser = new JsonParser();
    private final Class<T> clazz;

    public JsonResponseReader(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T read(HttpURLConnection uc) throws IOException {
        return parser.parse(uc.getResponseCode() < 300 ? uc.getInputStream() : uc.getErrorStream(), clazz);
    }
}
