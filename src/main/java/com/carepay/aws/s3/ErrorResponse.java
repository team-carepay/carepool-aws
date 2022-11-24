package com.carepay.aws.s3;

public class ErrorResponse {
    private String code;
    private String message;
    private String resource;
    private String requestId;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getResource() {
        return resource;
    }

    public String getRequestId() {
        return requestId;
    }
}
