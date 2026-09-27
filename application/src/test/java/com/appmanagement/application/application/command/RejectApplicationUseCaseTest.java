package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.ApplicationRejectedEvent;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.Reason;
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

class RejectApplicationUseCaseTest {

    private static final Reason REASON = new Reason("Risk policy violation");

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final AuditLog auditLog = mock(AuditLog.class);
    private final RejectApplicationUseCase useCase = new RejectApplicationUseCase(repository, auditLog);

    private Application verifiedApplication() {
        return Application.create(new Name("Loan request"), new Body("Body text")).next()
                .verify().next();
    }

    @Test
    void loadsRejectsSavesAuditsWithReasonAndReturnsPostSaveAggregate() {
        var loaded = verifiedApplication();
        var id = loaded.getId();
        when(repository.findById(id)).thenReturn(Optional.of(loaded));
        var postSave = loaded.reject(REASON).next();
        when(repository.save(any(Application.class))).thenReturn(postSave);
        var actor = new Actor("dave");

        var result = useCase.execute(new RejectApplicationUseCase.Params(id, REASON, actor));

        verify(repository).save(argThat(saved -> saved.getState() == ApplicationState.REJECTED));
        assertSame(postSave, result);
        verify(auditLog).append(actor, new ApplicationRejectedEvent(id, ApplicationState.VERIFIED, REASON));
    }

    @Test
    void unknownIdThrowsApplicationNotFound() {
        var id = ApplicationId.generate();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class,
                () -> useCase.execute(new RejectApplicationUseCase.Params(id, REASON, Actor.system())));

        verifyNoInteractions(auditLog);
    }
}