package com.appmanagement.infrastructure.application;

import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.events.TransitionEvent;
import com.appmanagement.infrastructure.AuditRecordJpaEntityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class JpaApplicationQueries implements ApplicationQueries {

    private static final Sort LISTING_SORT = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ApplicationJpaEntityRepository applications;
    private final AuditRecordJpaEntityRepository auditRecords;
    private final ApplicationQueryMapper mapper;

    public JpaApplicationQueries(ApplicationJpaEntityRepository applications,
                                 AuditRecordJpaEntityRepository auditRecords,
                                 ApplicationQueryMapper mapper) {
        this.applications = applications;
        this.auditRecords = auditRecords;
        this.mapper = mapper;
    }

    @Override
    public PageDto<ApplicationSummaryDto> search(ApplicationQueries.Search query) {
        var specification = Specification.allOf(
                nameContains(query.name().orElse(null)),
                visibleInListing(query.state().orElse(null)));
        var pageable = PageRequest.of(query.page(), query.size(), LISTING_SORT);
        var page = applications.findBy(
                specification,
                fluent -> fluent
                        .project("id", "name", "state", "publicationId", "createdAt", "updatedAt")
                        .as(ApplicationSummaryView.class)
                        .page(pageable));
        return mapper.toPageDto(page.map(mapper::toSummaryDto));
    }

    @Override
    public Optional<ApplicationDto> getById(ApplicationQueries.GetById query) {
        return applications.findById(query.id().value()).map(mapper::toDto);
    }

    @Override
    public PageDto<HistoryEntryDto> history(ApplicationQueries.History query) {
        var page = auditRecords.findByAggregateTypeAndAggregateIdOrderByOccurredAtAscIdAsc(
                TransitionEvent.AGGREGATE_TYPE,
                query.id().value().toString(),
                PageRequest.of(query.page(), query.size()));
        return mapper.toPageDto(page.map(mapper::toDto));
    }

    private static Specification<ApplicationJpaEntity> nameContains(String name) {
        if (name == null) {
            return null;
        }
        var sanitized = name.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return (root, query, builder) -> builder.like(
                builder.lower(root.get("name")), "%" + sanitized + "%", '\\');
    }

    private static Specification<ApplicationJpaEntity> stateEquals(ApplicationState state) {
        return (root, query, builder) -> builder.equal(root.get("state"), state);
    }

    private static Specification<ApplicationJpaEntity> visibleInListing(ApplicationState stateFilter) {
        if (stateFilter == null) {
            return (root, query, builder) -> builder.notEqual(root.get("state"), ApplicationState.DELETED);
        }
        return stateEquals(stateFilter);
    }
}