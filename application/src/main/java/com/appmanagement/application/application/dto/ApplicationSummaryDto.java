package com.appmanagement.application.application.dto;

import com.appmanagement.domain.application.ApplicationState;

import java.time.Instant;

public record ApplicationSummaryDto(String id, String name, ApplicationState state,
                                    Long publicationId, Instant createdAt, Instant updatedAt) {
}