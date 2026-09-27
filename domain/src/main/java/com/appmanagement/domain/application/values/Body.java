package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record Body(String value) {

    public static final int MAX_LENGTH = 10000;

    public Body {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException("body must not be blank and must be at most " + MAX_LENGTH + " characters long");
        }
    }
}