package com.appmanagement.infrastructure.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(SequencePublicationIdGenerator.class)
class SequencePublicationIdGeneratorTest {

    @Autowired
    private SequencePublicationIdGenerator generator;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    void createSequence() {
        testEntityManager.getEntityManager()
                .createNativeQuery("CREATE SEQUENCE IF NOT EXISTS publication_id_seq START WITH 1 INCREMENT BY 1")
                .executeUpdate();
    }

    @Test
    void returnsDistinctIncreasingValues() {
        var first = generator.next().value();
        var second = generator.next().value();
        var third = generator.next().value();

        assertTrue(second > first);
        assertTrue(third > second);
        assertTrue(first >= 1);
    }
}