package com.carepay.aws.ec2;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private ArrayList<Error> errors = new ArrayList<>();

    public List<Error> getErrors() {
        return errors;
    }
}
