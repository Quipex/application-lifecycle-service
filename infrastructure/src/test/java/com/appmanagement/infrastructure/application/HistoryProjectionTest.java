package com.appmanagement.infrastructure.application;

import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.infrastructure.AuditRecordJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({JpaApplicationQueries.class, ApplicationQueryMapper.class})
class HistoryProjectionTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JpaApplicationQueries queries;

    @Autowired
    private TestEntityManager testEntityManager;

    private long persistAuditRow(String aggregateId, String event, String stateFrom, String stateTo,
                                 String reason, Instant occurredAt) {
        var row = new AuditRecordJpaEntity(
                "APPLICATION", aggregateId, "system", event, stateFrom, stateTo, reason, occurredAt);
        testEntityManager.persistAndFlush(row);
        return row.getId();
    }

    @Test
    void historyReturnsOrderedEntriesForOneAggregateOnly() {
        var id = ApplicationId.generate();
        var aggregateId = id.value().toString();
        persistAuditRow(aggregateId, "CREATE", null, "CREATED", null, BASE);
        persistAuditRow(aggregateId, "EDIT", "CREATED", "CREATED", null, BASE.plusSeconds(1));
        persistAuditRow(aggregateId, "VERIFY", "CREATED", "VERIFIED", null, BASE.plusSeconds(2));
        persistAuditRow(ApplicationId.generate().value().toString(), "CREATE", null, "CREATED", null, BASE.plusSeconds(3));

        var history = queries.history(new ApplicationQueries.History(id, 0, 10));

        assertEquals(3, history.content().size());
        assertEquals(3, history.totalElements());
        assertEquals(ApplicationEvent.CREATE, history.content().get(0).event());
        assertNull(history.content().get(0).stateFrom());
        assertEquals(ApplicationState.CREATED, history.content().get(0).stateTo());
        assertNull(history.content().get(0).reason());
        assertEquals(ApplicationEvent.EDIT, history.content().get(1).event());
        assertEquals(ApplicationEvent.VERIFY, history.content().get(2).event());
        assertEquals(ApplicationState.VERIFIED, history.content().get(2).stateTo());
        assertEquals(BASE, history.content().get(0).occurredAt());
        assertEquals(BASE.plusSeconds(2), history.content().get(2).occurredAt());
    }

    @Test
    void historyPaginationKeepsStableOrderAndIncludesAcceptAndBothRejectPaths() {
        var id = ApplicationId.generate();
        var aggregateId = id.value().toString();
        var createId = persistAuditRow(aggregateId, "CREATE", null, "CREATED", null, BASE);
        var verifyId = persistAuditRow(aggregateId, "VERIFY", "CREATED", "VERIFIED", null, BASE);
        persistAuditRow(aggregateId, "ACCEPT", "VERIFIED", "ACCEPTED", null, BASE.plusSeconds(1));
        persistAuditRow(aggregateId, "REJECT", "VERIFIED", "REJECTED", "verified rejection", BASE.plusSeconds(2));
        persistAuditRow(aggregateId, "REJECT", "ACCEPTED", "REJECTED", "accepted rejection", BASE.plusSeconds(3));
        persistAuditRow(ApplicationId.generate().value().toString(), "CREATE", null, "CREATED", null,
                BASE.plusSeconds(4));

        var firstPage = queries.history(new ApplicationQueries.History(id, 0, 3));
        var secondPage = queries.history(new ApplicationQueries.History(id, 1, 3));

        assertTrue(createId < verifyId);
        assertEquals(0, firstPage.page());
        assertEquals(3, firstPage.size());
        assertEquals(5, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(List.of(ApplicationEvent.CREATE, ApplicationEvent.VERIFY, ApplicationEvent.ACCEPT),
                firstPage.content().stream().map(HistoryEntryDto::event).toList());
        assertEquals(1, secondPage.page());
        assertEquals(List.of(ApplicationEvent.REJECT, ApplicationEvent.REJECT),
                secondPage.content().stream().map(HistoryEntryDto::event).toList());
        assertEquals(ApplicationState.VERIFIED, secondPage.content().get(0).stateFrom());
        assertEquals("verified rejection", secondPage.content().get(0).reason());
        assertEquals(ApplicationState.ACCEPTED, secondPage.content().get(1).stateFrom());
        assertEquals("accepted rejection", secondPage.content().get(1).reason());
    }

    @Test
    void reasonIsPresentForDeleteAndRejectRowsOnly() {
        var id = ApplicationId.generate();
        var aggregateId = id.value().toString();
        persistAuditRow(aggregateId, "CREATE", null, "CREATED", null, BASE);
        persistAuditRow(aggregateId, "DELETE", "CREATED", "DELETED", "obsolete request", BASE.plusSeconds(1));
        persistAuditRow(aggregateId, "REJECT", "VERIFIED", "REJECTED", "risk policy", BASE.plusSeconds(2));

        var history = queries.history(new ApplicationQueries.History(id, 0, 10));

        assertNull(history.content().get(0).reason());
        assertEquals("obsolete request", history.content().get(1).reason());
        assertEquals("risk policy", history.content().get(2).reason());
        assertEquals(ApplicationState.DELETED, history.content().get(1).stateTo());
    }
}
