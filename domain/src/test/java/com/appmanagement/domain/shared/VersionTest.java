package com.appmanagement.domain.shared;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionTest {

    @Test
    void initialVersionIsZero() {
        assertEquals(0, Version.initial().value());
    }

    @Test
    void acceptsZeroAndPositiveValues() {
        assertEquals(0, new Version(0).value());
        assertEquals(7, new Version(7).value());
    }

    @Test
    void rejectsNegativeValue() {
        assertThrows(DomainRuleViolationException.class, () -> new Version(-1));
    }
}