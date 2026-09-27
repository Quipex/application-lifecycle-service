package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BodyTest {

    @Test
    void acceptsValueWithinLimits() {
        assertEquals("text", new Body("text").value());
        assertEquals("a".repeat(10000), new Body("a".repeat(10000)).value());
    }

    @Test
    void rejectsNull() {
        assertThrows(DomainRuleViolationException.class, () -> new Body(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(DomainRuleViolationException.class, () -> new Body(""));
        assertThrows(DomainRuleViolationException.class, () -> new Body("\t\n"));
    }

    @Test
    void rejectsValueOverLimit() {
        assertThrows(DomainRuleViolationException.class, () -> new Body("a".repeat(10001)));
        assertDoesNotThrow(() -> new Body("a".repeat(10000)));
    }
}