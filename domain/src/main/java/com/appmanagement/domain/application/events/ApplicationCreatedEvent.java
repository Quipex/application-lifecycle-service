package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public record ApplicationCreatedEvent(ApplicationId aggregateId) implements TransitionEvent {

    @Override
    public ApplicationEvent event() {
        return ApplicationEvent.CREATE;
    }

    @Override
    public Optional<ApplicationState> stateFrom() {
        return Optional.empty();
    }

    @Override
    public ApplicationState stateTo() {
        return ApplicationState.CREATED;
    }

    @Override
    public Optional<Reason> reason() {
        return Optional.empty();
    }
}