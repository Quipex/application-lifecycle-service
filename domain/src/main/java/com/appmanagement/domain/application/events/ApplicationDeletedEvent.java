package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public record ApplicationDeletedEvent(ApplicationId aggregateId, Reason deletionReason) implements TransitionEvent {

    @Override
    public ApplicationEvent event() {
        return ApplicationEvent.DELETE;
    }

    @Override
    public Optional<ApplicationState> stateFrom() {
        return Optional.of(ApplicationState.CREATED);
    }

    @Override
    public ApplicationState stateTo() {
        return ApplicationState.DELETED;
    }

    @Override
    public Optional<Reason> reason() {
        return Optional.of(deletionReason);
    }
}