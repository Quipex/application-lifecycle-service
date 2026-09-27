package com.appmanagement.infrastructure.application;

import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.values.ApplicationId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApplicationJpaRepository implements ApplicationRepository {

    private final ApplicationJpaEntityRepository applications;
    private final ApplicationMapper mapper;

    public ApplicationJpaRepository(ApplicationJpaEntityRepository applications, ApplicationMapper mapper) {
        this.applications = applications;
        this.mapper = mapper;
    }

    @Override
    public Optional<Application> findById(ApplicationId id) {
        return applications.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Application save(Application application) {
        var saved = applications.saveAndFlush(mapper.toEntity(application));
        return mapper.toDomain(saved);
    }
}