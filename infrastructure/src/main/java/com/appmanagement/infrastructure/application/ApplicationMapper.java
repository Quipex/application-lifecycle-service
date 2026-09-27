package com.appmanagement.infrastructure.application;

import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.shared.Version;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApplicationMapper {

    public Application toDomain(ApplicationJpaEntity entity) {
        return Application.builder()
                .id(new ApplicationId(entity.getId()))
                .name(new Name(entity.getName()))
                .body(new Body(entity.getBody()))
                .state(entity.getState())
                .publicationId(Optional.ofNullable(entity.getPublicationId()).map(PublicationId::new))
                .version(new Version(entity.getVersion()))
                .createdAt(Optional.ofNullable(entity.getCreatedAt()))
                .updatedAt(Optional.ofNullable(entity.getUpdatedAt()))
                .build();
    }

    public ApplicationJpaEntity toEntity(Application application) {
        return new ApplicationJpaEntity(
                application.getId().value(),
                application.getName().value(),
                application.getBody().value(),
                application.getState(),
                application.getPublicationId().map(PublicationId::value).orElse(null),
                application.getVersion().value(),
                application.getCreatedAt().orElse(null),
                application.getUpdatedAt().orElse(null));
    }
}