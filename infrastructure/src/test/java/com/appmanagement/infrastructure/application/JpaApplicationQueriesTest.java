package com.appmanagement.infrastructure.application;

import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({JpaApplicationQueries.class, ApplicationQueryMapper.class, ProjectionResultSetInspector.class})
class JpaApplicationQueriesTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JpaApplicationQueries queries;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private ResultColumnCapture resultColumnCapture;

    private UUID insertRow(String name, ApplicationState state, int createdAtEpochSecond) {
        var id = UUID.randomUUID();
        var entity = new ApplicationJpaEntity(
                id, name, "body of " + name, state, state == ApplicationState.PUBLISHED ? (long) createdAtEpochSecond : null,
                0L, BASE, BASE);
        testEntityManager.persistAndFlush(entity);
        testEntityManager.getEntityManager()
                .createNativeQuery("UPDATE applications SET created_at = :stamp, updated_at = :stamp WHERE id = :id")
                .setParameter("stamp", OffsetDateTime.ofInstant(BASE.plusSeconds(createdAtEpochSecond), ZoneOffset.UTC))
                .setParameter("id", id)
                .executeUpdate();
        testEntityManager.clear();
        return id;
    }

    private PageDto<ApplicationSummaryDto> search(String name, ApplicationState state, int page, int size) {
        return queries.search(new ApplicationQueries.Search(
                Optional.ofNullable(name), Optional.ofNullable(state), page, size));
    }

    private List<String> ids(PageDto<ApplicationSummaryDto> page) {
        return page.content().stream().map(ApplicationSummaryDto::id).toList();
    }

    @Test
    void deletedRowsAreHiddenFromDefaultListing() {
        var visible = insertRow("Visible request", ApplicationState.CREATED, 1);
        var deleted = insertRow("Deleted request", ApplicationState.DELETED, 2);
        var published = insertRow("Published request", ApplicationState.PUBLISHED, 3);

        var listed = ids(search(null, null, 0, 10));

        assertEquals(List.of(published.toString(), visible.toString()), listed);
    }

    @Test
    void deletedRowsAreReturnedForDeletedStateFilter() {
        var deleted = insertRow("Deleted request", ApplicationState.DELETED, 2);
        insertRow("Visible request", ApplicationState.CREATED, 1);

        var listed = ids(search(null, ApplicationState.DELETED, 0, 10));

        assertEquals(List.of(deleted.toString()), listed);
    }

    @Test
    void stateFilterReturnsOnlyMatchingRows() {
        var created = insertRow("First", ApplicationState.CREATED, 1);
        var verified = insertRow("Second", ApplicationState.VERIFIED, 2);

        assertEquals(List.of(created.toString()), ids(search(null, ApplicationState.CREATED, 0, 10)));
        assertEquals(List.of(verified.toString()), ids(search(null, ApplicationState.VERIFIED, 0, 10)));
    }

    @Test
    void nameFilterMatchesCaseInsensitiveSubstring() {
        insertRow("Quarterly Report", ApplicationState.CREATED, 1);
        insertRow("quarterly budget", ApplicationState.CREATED, 2);
        insertRow("Unrelated note", ApplicationState.CREATED, 3);

        var page = search("QUARTERLY", null, 0, 10);

        assertEquals(2, page.content().size());
        assertEquals(2, page.totalElements());
        assertEquals(1, page.totalPages());
    }

    @Test
    void nameFilterEscapesLikeWildcards() {
        var withPercent = insertRow("100% off sale", ApplicationState.CREATED, 1);
        insertRow("100 off sale", ApplicationState.CREATED, 2);
        var withUnderscore = insertRow("snake_case", ApplicationState.CREATED, 3);
        insertRow("snakexcase", ApplicationState.CREATED, 4);

        assertEquals(List.of(withPercent.toString()), ids(search("100%", null, 0, 10)));
        assertEquals(List.of(withUnderscore.toString()), ids(search("snake_", null, 0, 10)));
    }

    @Test
    void nameAndStateFiltersCombine() {
        var reportCreated = insertRow("Quarterly Report", ApplicationState.CREATED, 1);
        insertRow("Quarterly Report", ApplicationState.VERIFIED, 2);
        var budgetCreated = insertRow("Quarterly budget", ApplicationState.CREATED, 3);

        assertEquals(List.of(budgetCreated.toString(), reportCreated.toString()),
                ids(search("quarterly", ApplicationState.CREATED, 0, 10)));
    }

    @Test
    void defaultListingSortsByCreatedAtDescThenIdDesc() {
        var oldest = insertRow("Oldest", ApplicationState.CREATED, 1);
        var middle = insertRow("Middle", ApplicationState.CREATED, 2);
        var newest = insertRow("Newest", ApplicationState.CREATED, 3);

        assertEquals(List.of(newest.toString(), middle.toString(), oldest.toString()), ids(search(null, null, 0, 10)));
    }

    @Test
    void equalCreatedAtSortsByIdDesc() {
        var lowerId = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        var higherId = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        for (var id : List.of(lowerId, higherId)) {
            var entity = new ApplicationJpaEntity(
                    id, "same second " + id, "body", ApplicationState.CREATED, null, 0L, BASE, BASE);
            testEntityManager.persistAndFlush(entity);
            testEntityManager.getEntityManager()
                    .createNativeQuery("UPDATE applications SET created_at = :stamp, updated_at = :stamp WHERE id = :id")
                    .setParameter("stamp", OffsetDateTime.ofInstant(BASE, ZoneOffset.UTC))
                    .setParameter("id", id)
                    .executeUpdate();
            testEntityManager.clear();
        }

        assertEquals(List.of(higherId.toString(), lowerId.toString()), ids(search(null, null, 0, 10)));
    }

    @Test
    void paginationSplitsAndCountsPages() {
        insertRow("Row 1", ApplicationState.CREATED, 1);
        insertRow("Row 2", ApplicationState.CREATED, 2);
        insertRow("Row 3", ApplicationState.CREATED, 3);

        var firstPage = search(null, null, 0, 2);
        var secondPage = search(null, null, 1, 2);

        assertEquals(0, firstPage.page());
        assertEquals(2, firstPage.size());
        assertEquals(2, firstPage.content().size());
        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(1, secondPage.page());
        assertEquals(1, secondPage.content().size());
        assertEquals(List.of("Row 1"), secondPage.content().stream().map(ApplicationSummaryDto::name).toList());
    }

    @Test
    void summaryProjectionReturnsOnlyTheSixSummaryColumnsAndExcludesBody() {
        insertRow("Projected request", ApplicationState.CREATED, 1);
        resultColumnCapture.clear();

        search(null, null, 0, 10);

        var resultColumnCounts = resultColumnCapture.resultColumnCounts();
        assertTrue(resultColumnCounts.contains(6));
        assertFalse(resultColumnCounts.contains(8));
    }

    @Test
    void getByIdReturnsFullDtoWithBody() {
        var id = insertRow("Full request", ApplicationState.CREATED, 1);

        var dto = queries.getById(
                new ApplicationQueries.GetById(new ApplicationId(id))).orElseThrow();

        assertEquals(id.toString(), dto.id());
        assertEquals("Full request", dto.name());
        assertEquals("body of Full request", dto.body());
        assertEquals(ApplicationState.CREATED, dto.state());
        assertEquals(0, dto.version());
    }

    @Test
    void getByIdReturnsEmptyForUnknownId() {
        var id = UUID.randomUUID();

        assertEquals(Optional.empty(), queries.getById(
                new ApplicationQueries.GetById(new ApplicationId(id))));
    }
}