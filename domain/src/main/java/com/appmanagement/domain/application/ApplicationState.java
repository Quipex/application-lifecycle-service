package com.appmanagement.domain.application;

public enum ApplicationState {
    CREATED,
    VERIFIED,
    ACCEPTED,
    PUBLISHED,
    DELETED,
    REJECTED;

    public ApplicationState transition(ApplicationEvent event) {
        return ApplicationStateMachine.transition(this, event);
    }
}