package com.appmanagement.domain.application.events;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Reason;

import java.util.Optional;

public sealed interface TransitionEvent permits
        ApplicationCreatedEvent,
        ApplicationVerifiedEvent,
        ApplicationDeletedEvent,
        ApplicationAcceptedEvent,
        ApplicationRejectedEvent,
        ApplicationPublishedEvent,
        ApplicationBodyEditedEvent {

    String AGGREGATE_TYPE = "APPLICATION";

    default String aggregateType() {
        return AGGREGATE_TYPE;
    }

    ApplicationId aggregateId();

    ApplicationEvent event();

    Optional<ApplicationState> stateFrom();

    ApplicationState stateTo();

    Optional<Reason> reason();
}