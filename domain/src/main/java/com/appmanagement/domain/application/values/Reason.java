package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record Reason(String value) {

    public static final int MAX_LENGTH = 512;

    public Reason {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException("reason must not be blank and must be at most " + MAX_LENGTH + " characters long");
        }
    }
}