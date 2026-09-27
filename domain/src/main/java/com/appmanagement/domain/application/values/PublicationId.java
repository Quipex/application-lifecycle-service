package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;

public record PublicationId(long value) {

    public PublicationId {
        if (value <= 0) {
            throw new DomainRuleViolationException("publication id must be a positive number");
        }
    }
}