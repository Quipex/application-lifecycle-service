package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.ApplicationPublishedEvent;
import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.PublicationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublishApplicationUseCaseTest {

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final AuditLog auditLog = mock(AuditLog.class);
    private final PublicationIdGenerator publicationIdGenerator = mock(PublicationIdGenerator.class);
    private final PublishApplicationUseCase useCase =
            new PublishApplicationUseCase(repository, auditLog, publicationIdGenerator);

    private Application acceptedApplication() {
        return Application.create(new Name("Loan request"), new Body("Body text")).next()
                .verify().next()
                .accept().next();
    }

    @Test
    void loadsPublishesSavesAuditsAndReturnsPostSaveAggregate() {
        var loaded = acceptedApplication();
        var id = loaded.getId();
        when(repository.findById(id)).thenReturn(Optional.of(loaded));
        when(publicationIdGenerator.next()).thenReturn(new PublicationId(42));
        var postSave = loaded.publish(publicationIdGenerator).next();
        when(repository.save(any(Application.class))).thenReturn(postSave);
        var actor = new Actor("erin");

        var result = useCase.execute(new PublishApplicationUseCase.Params(id, actor));

        verify(repository).save(argThat(saved -> saved.getState() == ApplicationState.PUBLISHED
                && saved.getPublicationId().equals(Optional.of(new PublicationId(42)))));
        assertSame(postSave, result);
        verify(auditLog).append(actor, new ApplicationPublishedEvent(id, new PublicationId(42)));
    }

    @Test
    void unknownIdThrowsApplicationNotFound() {
        var id = ApplicationId.generate();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
                () -> useCase.execute(new PublishApplicationUseCase.Params(id, Actor.system())));

        verifyNoInteractions(auditLog);
    }
}