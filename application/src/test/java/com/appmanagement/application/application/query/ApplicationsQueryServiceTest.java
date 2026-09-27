package com.appmanagement.application.application.query;

import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.values.ApplicationId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationsQueryServiceTest {

    private final ApplicationQueries queries = mock(ApplicationQueries.class);
    private final ApplicationsQueryService service = new ApplicationsQueryService(queries);

    @Test
    void historyForUnknownIdThrowsNotFound() {
        var id = ApplicationId.generate();
        var history = new ApplicationQueries.History(id, 0, 10);
        when(queries.getById(new ApplicationQueries.GetById(id))).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> service.history(history));

        verify(queries, never()).history(history);
    }

    @Test
    void historyForKnownIdReturnsPage() {
        var id = ApplicationId.generate();
        when(queries.getById(new ApplicationQueries.GetById(id)))
                .thenReturn(Optional.of(new ApplicationDto(id.value().toString(), "Name", "Body", null,
                        null, 0, null, null)));
        PageDto<HistoryEntryDto> entries = new PageDto<>(List.of(), 0, 10, 0, 0);
        var history = new ApplicationQueries.History(id, 0, 10);
        when(queries.history(history)).thenReturn(entries);

        var result = service.history(history);

        assertEquals(entries, result);
    }
}
