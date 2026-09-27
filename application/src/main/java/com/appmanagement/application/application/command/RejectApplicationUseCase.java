package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RejectApplicationUseCase {

    private final ApplicationRepository repository;
    private final AuditLog auditLog;

    public RejectApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    public record Params(ApplicationId id, Reason reason, Actor actor) {
    }

    public Application execute(Params params) {
        var application = load(params.id());
        var result = application.reject(params.reason());
        var saved = repository.save(result.next());
        auditLog.append(params.actor(), result.event());
        return saved;
    }

    private Application load(ApplicationId id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}