package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReasonTest {

    @Test
    void acceptsValueWithinLimits() {
        assertEquals("duplicate request", new Reason("duplicate request").value());
        assertEquals("a".repeat(512), new Reason("a".repeat(512)).value());
    }

    @Test
    void rejectsNull() {
        assertThrows(DomainRuleViolationException.class, () -> new Reason(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(DomainRuleViolationException.class, () -> new Reason(""));
        assertThrows(DomainRuleViolationException.class, () -> new Reason("  "));
    }

    @Test
    void rejectsValueOverLimit() {
        assertThrows(DomainRuleViolationException.class, () -> new Reason("a".repeat(513)));
        assertDoesNotThrow(() -> new Reason("a".repeat(512)));
    }
}