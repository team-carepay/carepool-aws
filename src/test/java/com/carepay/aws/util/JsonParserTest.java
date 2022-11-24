package com.carepay.aws.util;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonParserTest {

    @Test
    void parse() throws IOException {
        JsonParser parser = new JsonParser();
        EC2Credentials creds = parser.parse(getClass().getResourceAsStream("/ec2-credentials.json"), EC2Credentials.class);
        assertThat(creds.accessKeyId).isEqualTo("AKIDEXAMPLE");
    }

    @Test
    void parseArray() throws IOException {
        JsonParser parser = new JsonParser();
        TestData testData = parser.parse(getClass().getResourceAsStream("/sample.json"), TestData.class);
        assertThat(testData.persons).hasSize(2);
    }

    static class EC2Credentials {
        String accessKeyId;
        String secretAccessKey;
        String token;
        Instant expiration;
        Instant lastUpdated;
        String code;
    }

    public static class TestData {
        private String id;
        private ArrayList<Person> persons;

        public ArrayList<Person> getPersons() {
            return persons;
        }
    }

    public static class Person {
        private String name;
        private int age;
        private boolean married;
        private long total;
        private float average;
        private double percentage;
    }
}