package com.appmanagement.infrastructure.application;

import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import com.appmanagement.domain.application.values.PublicationId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class SequencePublicationIdGenerator implements PublicationIdGenerator {

    private final EntityManager entityManager;

    public SequencePublicationIdGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public PublicationId next() {
        var value = entityManager
                .createNativeQuery("SELECT NEXT VALUE FOR publication_id_seq")
                .getSingleResult();
        return new PublicationId(((Number) value).longValue());
    }
}