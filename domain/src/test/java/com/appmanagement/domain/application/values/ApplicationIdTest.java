package com.appmanagement.domain.application.values;

import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationIdTest {

    @Test
    void wrapsTheUnderlyingUuid() {
        var uuid = UUID.randomUUID();
        assertEquals(uuid, new ApplicationId(uuid).value());
    }

    @Test
    void rejectsNull() {
        assertThrows(DomainRuleViolationException.class, () -> new ApplicationId(null));
    }

    @Test
    void generateProducesFreshIds() {
        var first = ApplicationId.generate();
        var second = ApplicationId.generate();

        assertNotEquals(first, second);
        assertEquals(36, first.value().toString().length());
    }
}