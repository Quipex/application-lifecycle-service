package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

import java.util.UUID;

public record ApplicationId(UUID value) {

    public ApplicationId {
        if (value == null) {
            throw new DomainRuleViolationException("application id must be present");
        }
    }

    public static ApplicationId generate() {
        return new ApplicationId(UUID.randomUUID());
    }
}