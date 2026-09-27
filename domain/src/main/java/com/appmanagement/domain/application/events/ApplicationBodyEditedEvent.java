package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public record ApplicationBodyEditedEvent(ApplicationId aggregateId, ApplicationState state) implements TransitionEvent {

    @Override
    public ApplicationEvent event() {
        return ApplicationEvent.EDIT;
    }

    @Override
    public Optional<ApplicationState> stateFrom() {
        return Optional.of(state);
    }

    @Override
    public ApplicationState stateTo() {
        return state;
    }

    @Override
    public Optional<Reason> reason() {
        return Optional.empty();
    }
}