package com.appmanagement.application.application.query.ports;

import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;

import java.util.Optional;

public interface ApplicationQueries {

    record Search(Optional<String> name, Optional<ApplicationState> state, int page, int size) {
    }

    record GetById(ApplicationId id) {
    }

    record History(ApplicationId id, int page, int size) {
    }

    PageDto<ApplicationSummaryDto> search(Search query);

    Optional<ApplicationDto> getById(GetById query);

    PageDto<HistoryEntryDto> history(History query);
}
