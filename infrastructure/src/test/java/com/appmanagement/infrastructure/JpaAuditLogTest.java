package com.appmanagement.infrastructure;

import com.appmanagement.application.shared.Actor;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.ApplicationAcceptedEvent;
import com.appmanagement.domain.application.events.ApplicationBodyEditedEvent;
import com.appmanagement.domain.application.events.ApplicationCreatedEvent;
import com.appmanagement.domain.application.events.ApplicationDeletedEvent;
import com.appmanagement.domain.application.events.ApplicationPublishedEvent;
import com.appmanagement.domain.application.events.ApplicationRejectedEvent;
import com.appmanagement.domain.application.events.ApplicationVerifiedEvent;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.application.values.Reason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(JpaAuditLog.class)
class JpaAuditLogTest {

    private static final Instant TEST_CLOCK_INSTANT = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private JpaAuditLog auditLog;

    @Autowired
    private AuditRecordJpaEntityRepository auditRecords;

    private AuditRecordJpaEntity singleRow() {
        var all = auditRecords.findAll();
        assertEquals(1, all.size());
        return all.get(0);
    }

    private void assertRow(AuditRecordJpaEntity row, ApplicationId id, String actor, String event,
                           String stateFrom, String stateTo, String reason) {
        assertEquals("APPLICATION", row.getAggregateType());
        assertEquals(id.value().toString(), row.getAggregateId());
        assertEquals(actor, row.getActor());
        assertEquals(event, row.getEvent());
        assertEquals(stateFrom, row.getStateFrom());
        assertEquals(stateTo, row.getStateTo());
        assertEquals(reason, row.getReason());
        assertEquals(TEST_CLOCK_INSTANT, row.getOccurredAt());
    }

    @Test
    void appendMapsCreateEvent() {
        var id = ApplicationId.generate();
        auditLog.append(Actor.system(), new ApplicationCreatedEvent(id));

        assertRow(singleRow(), id, "system", "CREATE", null, "CREATED", null);
    }

    @Test
    void appendMapsVerifyEvent() {
        var id = ApplicationId.generate();
        auditLog.append(new Actor("alice"), new ApplicationVerifiedEvent(id));

        assertRow(singleRow(), id, "alice", "VERIFY", "CREATED", "VERIFIED", null);
    }

    @Test
    void appendMapsEditEvent() {
        var id = ApplicationId.generate();
        auditLog.append(new Actor("bob"), new ApplicationBodyEditedEvent(id, ApplicationState.VERIFIED));

        assertRow(singleRow(), id, "bob", "EDIT", "VERIFIED", "VERIFIED", null);
    }

    @Test
    void appendMapsDeleteEventWithReason() {
        var id = ApplicationId.generate();
        auditLog.append(new Actor("carol"), new ApplicationDeletedEvent(id, new Reason("obsolete request")));

        assertRow(singleRow(), id, "carol", "DELETE", "CREATED", "DELETED", "obsolete request");
    }

    @Test
    void appendMapsAcceptEvent() {
        var id = ApplicationId.generate();
        auditLog.append(new Actor("dave"), new ApplicationAcceptedEvent(id));

        assertRow(singleRow(), id, "dave", "ACCEPT", "VERIFIED", "ACCEPTED", null);
    }

    @Test
    void appendMapsRejectEventWithItsSourceStateAndReason() {
        var id = ApplicationId.generate();
        auditLog.append(new Actor("erin"), new ApplicationRejectedEvent(
                id, ApplicationState.ACCEPTED, new Reason("risk policy")));

        assertRow(singleRow(), id, "erin", "REJECT", "ACCEPTED", "REJECTED", "risk policy");
    }

    @Test
    void appendMapsPublishEventWithoutPersistingPublicationId() {
        var id = ApplicationId.generate();
        auditLog.append(Actor.system(), new ApplicationPublishedEvent(id, new PublicationId(42)));

        assertRow(singleRow(), id, "system", "PUBLISH", "ACCEPTED", "PUBLISHED", null);
    }
}
