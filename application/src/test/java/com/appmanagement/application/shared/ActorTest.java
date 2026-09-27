package com.appmanagement.application.shared;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActorTest {

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(DomainRuleViolationException.class, () -> new Actor(null));
        assertThrows(DomainRuleViolationException.class, () -> new Actor(" \t"));
    }

    @Test
    void acceptsMaximumLength() {
        var value = "a".repeat(Actor.MAX_LENGTH);

        assertEquals(value, new Actor(value).value());
    }

    @Test
    void rejectsValuesLongerThanMaximum() {
        assertThrows(DomainRuleViolationException.class, () -> new Actor("a".repeat(65)));
    }
}
