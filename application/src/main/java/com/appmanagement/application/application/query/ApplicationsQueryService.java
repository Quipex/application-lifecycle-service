package com.appmanagement.application.application.query;

import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class ApplicationsQueryService {

    private final ApplicationQueries queries;

    public ApplicationsQueryService(ApplicationQueries queries) {
        this.queries = queries;
    }

    public PageDto<ApplicationSummaryDto> search(ApplicationQueries.Search query) {
        return queries.search(query);
    }

    public ApplicationDto getById(ApplicationQueries.GetById query) {
        return queries.getById(query).orElseThrow(() -> new ApplicationNotFoundException(query.id()));
    }

    public PageDto<HistoryEntryDto> history(ApplicationQueries.History query) {
        queries.getById(new ApplicationQueries.GetById(query.id()))
                .orElseThrow(() -> new ApplicationNotFoundException(query.id()));
        return queries.history(query);
    }
}
