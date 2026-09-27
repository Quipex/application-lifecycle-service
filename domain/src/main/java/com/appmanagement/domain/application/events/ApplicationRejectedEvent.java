package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public record ApplicationRejectedEvent(ApplicationId aggregateId, ApplicationState from, Reason rejectionReason) implements TransitionEvent {

    @Override
    public ApplicationEvent event() {
        return ApplicationEvent.REJECT;
    }

    @Override
    public Optional<ApplicationState> stateFrom() {
        return Optional.of(from);
    }

    @Override
    public ApplicationState stateTo() {
        return ApplicationState.REJECTED;
    }

    @Override
    public Optional<Reason> reason() {
        return Optional.of(rejectionReason);
    }
}