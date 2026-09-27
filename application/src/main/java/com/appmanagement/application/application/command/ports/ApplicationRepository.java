package com.appmanagement.application.application.command.ports;

import com.appmanagement.domain.application.Application;
import com.appmanagement.domain.application.values.ApplicationId;

import java.util.Optional;

public interface ApplicationRepository {

    Optional<Application> findById(ApplicationId id);

    Application save(Application application);
}