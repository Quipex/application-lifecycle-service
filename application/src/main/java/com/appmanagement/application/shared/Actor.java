package com.appmanagement.application.shared;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record Actor(String value) {

    public static final int MAX_LENGTH = 64;
    public static final String SYSTEM_ACTOR = "system";

    public Actor {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException("actor must not be blank and must be at most " + MAX_LENGTH + " characters long");
        }
    }

    public static Actor system() {
        return new Actor(SYSTEM_ACTOR);
    }
}