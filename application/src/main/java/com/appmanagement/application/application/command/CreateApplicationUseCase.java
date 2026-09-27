package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CreateApplicationUseCase {

    private final ApplicationRepository repository;
    private final AuditLog auditLog;
    public CreateApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    public record Params(Name name, Body body, Actor actor) {
    }

    public Application execute(Params params) {
        var result = Application.create(params.name(), params.body());
        var saved = repository.save(result.next());
        auditLog.append(params.actor(), result.event());
        return saved;
    }
}