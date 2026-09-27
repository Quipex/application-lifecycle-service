package com.appmanagement.infrastructure;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.TransitionEvent;
import com.appmanagement.domain.application.values.Reason;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class JpaAuditLog implements AuditLog {

    private final AuditRecordJpaEntityRepository auditRecords;
    private final Clock clock;

    public JpaAuditLog(AuditRecordJpaEntityRepository auditRecords, Clock clock) {
        this.auditRecords = auditRecords;
        this.clock = clock;
    }

    @Override
    public void append(Actor actor, TransitionEvent event) {
        auditRecords.save(new AuditRecordJpaEntity(
                event.aggregateType(),
                event.aggregateId().value().toString(),
                actor.value(),
                event.event().name(),
                event.stateFrom().map(ApplicationState::name).orElse(null),
                event.stateTo().name(),
                event.reason().map(Reason::value).orElse(null),
                clock.instant()));
    }
}