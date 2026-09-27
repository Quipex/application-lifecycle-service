package com.appmanagement.application.application.dto;

import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.PublicationId;

import java.time.Instant;

public record ApplicationDto(String id, String name, String body, ApplicationState state,
                             Long publicationId, long version, Instant createdAt, Instant updatedAt) {

    public static ApplicationDto from(Application application) {
        return new ApplicationDto(
                application.getId().value().toString(),
                application.getName().value(),
                application.getBody().value(),
                application.getState(),
                application.getPublicationId().map(PublicationId::value).orElse(null),
                application.getVersion().value(),
                application.getCreatedAt().orElse(null),
                application.getUpdatedAt().orElse(null));
    }
}