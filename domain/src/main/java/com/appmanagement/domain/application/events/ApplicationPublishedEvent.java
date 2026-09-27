package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public record ApplicationPublishedEvent(ApplicationId aggregateId, PublicationId publicationId) implements TransitionEvent {

    @Override
    public ApplicationEvent event() {
        return ApplicationEvent.PUBLISH;
    }

    @Override
    public Optional<ApplicationState> stateFrom() {
        return Optional.of(ApplicationState.ACCEPTED);
    }

    @Override
    public ApplicationState stateTo() {
        return ApplicationState.PUBLISHED;
    }

    @Override
    public Optional<Reason> reason() {
        return Optional.empty();
    }
}