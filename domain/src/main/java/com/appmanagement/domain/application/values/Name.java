package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record Name(String value) {

    public static final int MAX_LENGTH = 100;

    public Name {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException("name must not be blank and must be at most " + MAX_LENGTH + " characters long");
        }
    }
}