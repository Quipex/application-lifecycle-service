package com.appmanagement.infrastructure.application;

import com.appmanagement.domain.application.ApplicationState;

import java.time.Instant;
import java.util.UUID;

public interface ApplicationSummaryView {

    UUID getId();

    String getName();

    ApplicationState getState();

    Long getPublicationId();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}