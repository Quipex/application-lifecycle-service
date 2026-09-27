package com.appmanagement.domain.shared;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record Version(long value) {

    public Version {
        if (value < 0) {
            throw new DomainRuleViolationException("version must not be negative");
        }
    }

    public static Version initial() {
        return new Version(0);
    }
}