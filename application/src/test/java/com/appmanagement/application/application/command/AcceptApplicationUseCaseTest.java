package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.ApplicationAcceptedEvent;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
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

class AcceptApplicationUseCaseTest {

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final AuditLog auditLog = mock(AuditLog.class);
    private final AcceptApplicationUseCase useCase = new AcceptApplicationUseCase(repository, auditLog);

    private Application verifiedApplication() {
        return Application.create(new Name("Loan request"), new Body("Body text")).next()
                .verify().next();
    }

    @Test
    void loadsAcceptsSavesAuditsAndReturnsPostSaveAggregate() {
        var loaded = verifiedApplication();
        var id = loaded.getId();
        when(repository.findById(id)).thenReturn(Optional.of(loaded));
        var postSave = loaded.accept().next();
        when(repository.save(any(Application.class))).thenReturn(postSave);
        var actor = new Actor("carol");

        var result = useCase.execute(new AcceptApplicationUseCase.Params(id, actor));

        verify(repository).save(argThat(saved -> saved.getState() == ApplicationState.ACCEPTED));
        assertSame(postSave, result);
        verify(auditLog).append(actor, new ApplicationAcceptedEvent(id));
    }

    @Test
    void unknownIdThrowsApplicationNotFound() {
        var id = ApplicationId.generate();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
                () -> useCase.execute(new AcceptApplicationUseCase.Params(id, Actor.system())));

        verifyNoInteractions(auditLog);
    }
}