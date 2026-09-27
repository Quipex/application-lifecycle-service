package com.appmanagement.infrastructure.application;

import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.shared.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({ApplicationJpaRepository.class, ApplicationMapper.class})
class ApplicationJpaRepositoryTest {

    private static final Instant PERSISTED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private ApplicationJpaRepository repository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private ApplicationMapper mapper;

    private Application createdApplication() {
        return Application.create(new Name("Loan request"), new Body("Body text")).next();
    }

    @Test
    void saveInsertsNewRowAndReturnsAggregateWithHibernateStampedValues() {
        var unsaved = createdApplication();
        assertTrue(unsaved.getCreatedAt().isEmpty());
        assertTrue(unsaved.getUpdatedAt().isEmpty());

        var saved = repository.save(unsaved);

        assertNotNull(saved.getId());
        assertEquals(ApplicationState.CREATED, saved.getState());
        assertTrue(saved.getPublicationId().isEmpty());
        assertEquals(0, saved.getVersion().value());
        assertTrue(saved.getCreatedAt().isPresent());
        assertTrue(saved.getUpdatedAt().isPresent());
    }

    @Test
    void findByIdRoundTripsValueObjects() {
        var saved = repository.save(createdApplication()
                .verify().next()
                .accept().next()
                .publish(() -> new PublicationId(42)).next());

        var loaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), loaded.getId());
        assertEquals(saved.getName(), loaded.getName());
        assertEquals(saved.getBody(), loaded.getBody());
        assertEquals(ApplicationState.PUBLISHED, loaded.getState());
        assertTrue(loaded.getPublicationId().isPresent());
        assertEquals(42, loaded.getPublicationId().orElseThrow().value());
        assertEquals(saved.getVersion(), loaded.getVersion());
        assertEquals(saved.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(saved.getUpdatedAt(), loaded.getUpdatedAt());
    }

    @Test
    void saveMergesExistingRowRefreshingUpdatedAtAndPreservingCreatedAt() throws InterruptedException {
        var saved = repository.save(createdApplication());
        var createdAt = saved.getCreatedAt().orElseThrow();
        var firstUpdatedAt = saved.getUpdatedAt().orElseThrow();
        Thread.sleep(50);

        var loaded = repository.findById(saved.getId()).orElseThrow();
        var merged = repository.save(loaded.verify().next());

        assertEquals(ApplicationState.VERIFIED, merged.getState());
        assertEquals(createdAt, merged.getCreatedAt().orElseThrow());
        assertTrue(merged.getUpdatedAt().orElseThrow().isAfter(firstUpdatedAt));
        assertEquals(1, merged.getVersion().value());

        testEntityManager.clear();
        var reloaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(ApplicationState.VERIFIED, reloaded.getState());
        assertEquals(createdAt, reloaded.getCreatedAt().orElseThrow());
        assertEquals(1, reloaded.getVersion().value());
    }

    @Test
    void publishedMapperRoundTripPreservesPublicationIdAndVersion() {
        var timestamp = PERSISTED_AT;
        var published = Application.builder()
                .id(com.appmanagement.domain.application.values.ApplicationId.generate())
                .name(new Name("Published request"))
                .body(new Body("Body text"))
                .state(ApplicationState.PUBLISHED)
                .publicationId(Optional.of(new PublicationId(42)))
                .version(new Version(7))
                .createdAt(Optional.of(timestamp))
                .updatedAt(Optional.of(timestamp))
                .build();

        var mapped = mapper.toDomain(mapper.toEntity(published));

        assertEquals(Optional.of(new PublicationId(42)), mapped.getPublicationId());
        assertEquals(new Version(7), mapped.getVersion());
    }

    @Test
    void saveWithStaleVersionThrowsOptimisticLockingFailure() {
        var created = repository.save(createdApplication());
        var loaded = repository.findById(created.getId()).orElseThrow();

        repository.save(loaded.verify().next());
        testEntityManager.clear();

        var staleCopy = loaded.verify().next();
        assertEquals(0, staleCopy.getVersion().value());

        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(staleCopy));
    }

}