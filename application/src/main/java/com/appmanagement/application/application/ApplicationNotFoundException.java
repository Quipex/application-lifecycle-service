package com.appmanagement.application.application;

import com.appmanagement.domain.application.values.ApplicationId;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(ApplicationId id) {
        super("application " + id.value() + " not found");
    }
}