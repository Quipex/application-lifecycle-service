package com.appmanagement.application.application.dto;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;

import java.time.Instant;

public record HistoryEntryDto(ApplicationEvent event, ApplicationState stateFrom, ApplicationState stateTo,
                              String reason, String actor, Instant occurredAt) {
}