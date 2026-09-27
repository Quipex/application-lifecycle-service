package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.ApplicationCreatedEvent;
import com.appmanagement.domain.application.events.TransitionEvent;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateApplicationUseCaseTest {

    private static final Instant PERSISTED_AT = Instant.parse("2026-01-01T12:00:00Z");

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final AuditLog auditLog = mock(AuditLog.class);
    private final CreateApplicationUseCase useCase = new CreateApplicationUseCase(repository, auditLog);

    @Test
    void createsWithoutTimestampsAndReturnsThePersistenceRefreshedAggregate() {
        var unsaved = Application.create(new Name("Loan request"), new Body("Body")).next();
        var postSave = unsaved.toBuilder()
                .createdAt(Optional.of(PERSISTED_AT))
                .updatedAt(Optional.of(PERSISTED_AT))
                .build();
        when(repository.save(any(Application.class))).thenReturn(postSave);

        var result = useCase.execute(
                new CreateApplicationUseCase.Params(new Name("Loan request"), new Body("Body"), Actor.system()));

        var savedCaptor = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(savedCaptor.capture());
        assertEquals(ApplicationState.CREATED, savedCaptor.getValue().getState());
        assertEquals(Optional.empty(), savedCaptor.getValue().getCreatedAt());
        assertEquals(Optional.empty(), savedCaptor.getValue().getUpdatedAt());

        var eventCaptor = ArgumentCaptor.forClass(TransitionEvent.class);
        verify(auditLog).append(eq(Actor.system()), eventCaptor.capture());
        var event = assertInstanceOf(ApplicationCreatedEvent.class, eventCaptor.getValue());
        assertEquals(savedCaptor.getValue().getId(), event.aggregateId());

        assertSame(postSave, result);
        assertEquals(Optional.of(PERSISTED_AT), result.getCreatedAt());
        assertEquals(Optional.of(PERSISTED_AT), result.getUpdatedAt());
    }
}
