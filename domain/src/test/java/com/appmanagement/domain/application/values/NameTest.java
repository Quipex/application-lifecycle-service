package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameTest {

    @Test
    void acceptsValueWithinLimits() {
        assertEquals("a", new Name("a").value());
        assertEquals("a".repeat(100), new Name("a".repeat(100)).value());
        assertEquals("  padded  ", new Name("  padded  ").value());
    }

    @Test
    void rejectsNull() {
        assertThrows(DomainRuleViolationException.class, () -> new Name(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(DomainRuleViolationException.class, () -> new Name(""));
        assertThrows(DomainRuleViolationException.class, () -> new Name("   "));
    }

    @Test
    void rejectsValueOverLimit() {
        assertThrows(DomainRuleViolationException.class, () -> new Name("a".repeat(101)));
        assertDoesNotThrow(() -> new Name("a".repeat(100)));
    }
}