package com.appmanagement.web.application;

import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.application.application.query.ApplicationsQueryService;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class ApplicationQueryController {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_OFFSET = 100000;
    static final int MAX_NAME_FILTER_LENGTH = 100;

    private final ApplicationsQueryService applicationsQuery;
    private final ApplicationListingProperties listingProperties;

    public ApplicationQueryController(ApplicationsQueryService applicationsQuery,
                                      ApplicationListingProperties listingProperties) {
        this.applicationsQuery = applicationsQuery;
        this.listingProperties = listingProperties;
    }

    @GetMapping
    public PageDto<ApplicationSummaryDto> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        var pageSize = size == null ? listingProperties.defaultPageSize() : size;
        validatePagination(page, pageSize);
        if (name != null && name.length() > MAX_NAME_FILTER_LENGTH) {
            throw new InvalidNameFilterException();
        }
        Optional<ApplicationState> stateFilter = state == null
                ? Optional.empty()
                : Optional.of(parseState(state));
        return applicationsQuery.search(new ApplicationQueries.Search(
                normalizeName(name), stateFilter, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApplicationDto get(@PathVariable UUID id) {
        return applicationsQuery.getById(new ApplicationQueries.GetById(new ApplicationId(id)));
    }

    @GetMapping("/{id}/history")
    public PageDto<HistoryEntryDto> history(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        var pageSize = size == null ? listingProperties.defaultPageSize() : size;
        validatePagination(page, pageSize);
        return applicationsQuery.history(new ApplicationQueries.History(new ApplicationId(id), page, pageSize));
    }

    private static void validatePagination(int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(page, pageSize);
        }
        var offset = (long) page * pageSize;
        if (offset > MAX_OFFSET) {
            throw new InvalidPaginationException(page, pageSize);
        }
    }

    private Optional<String> normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    private ApplicationState parseState(String state) {
        try {
            return ApplicationState.valueOf(state.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UnknownStateFilterException(state);
        }
    }
}
