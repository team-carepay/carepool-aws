package com.carepay.aws.util;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryStringUtilsTest {

    private Map<String, String> params = new TreeMap<>();

    @Test
    public void testToString() {
        params.put("Param1", "Value1");
        params.put("Param2", "Value2");
        assertThat(QueryStringUtils.toQueryString(params)).isEqualTo("Param1=Value1&Param2=Value2");
    }

    @Test
    public void testEscaping() {
        params.put("Param1", "\t-_~.\n€");
        assertThat(QueryStringUtils.toQueryString(params)).isEqualTo("Param1=%09-_~.%0A%E2%82%AC");
    }

    @Test
    public void parseQueryString() {
    }

    @Test
    public void uriEncode() {
        assertThat(QueryStringUtils.uriEncode("\t-_~.\n€/")).isEqualTo("%09-_~.%0A%E2%82%AC%2F");
    }

    @Test
    public void uriEncodePath() {
        assertThat(QueryStringUtils.uriEncodePath("/test/bla")).isEqualTo("/test/bla");
    }

    @Test
    public void uriDecode() {
        assertThat(QueryStringUtils.uriDecode("%09-_~.%0A%E2%82%AC%2F")).isEqualTo("\t-_~.\n€/");
    }
}
