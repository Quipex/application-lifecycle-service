package com.appmanagement.application.application.command;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import com.appmanagement.domain.application.values.ApplicationId;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PublishApplicationUseCase {

    private final ApplicationRepository repository;
    private final AuditLog auditLog;
    private final PublicationIdGenerator publicationIdGenerator;

    public PublishApplicationUseCase(ApplicationRepository repository, AuditLog auditLog,
                                     PublicationIdGenerator publicationIdGenerator) {
        this.repository = repository;
        this.auditLog = auditLog;
        this.publicationIdGenerator = publicationIdGenerator;
    }

    public record Params(ApplicationId id, Actor actor) {
    }

    public Application execute(Params params) {
        var application = load(params.id());
        var result = application.publish(publicationIdGenerator);
        var saved = repository.save(result.next());
        auditLog.append(params.actor(), result.event());
        return saved;
    }

    private Application load(ApplicationId id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}