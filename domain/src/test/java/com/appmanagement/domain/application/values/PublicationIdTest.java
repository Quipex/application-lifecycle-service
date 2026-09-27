package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicationIdTest {

    @Test
    void acceptsPositiveValue() {
        assertEquals(1, new PublicationId(1).value());
        assertEquals(Long.MAX_VALUE, new PublicationId(Long.MAX_VALUE).value());
    }

    @Test
    void rejectsZero() {
        assertThrows(DomainRuleViolationException.class, () -> new PublicationId(0));
    }

    @Test
    void rejectsNegativeValue() {
        assertThrows(DomainRuleViolationException.class, () -> new PublicationId(-1));
    }
}