package com.appmanagement.domain.application;

import com.appmanagement.domain.application.exception.InvalidTransitionException;

import java.util.List;

public final class ApplicationStateMachine {

    private static final List<Transition> TRANSITIONS = List.of(
            new Transition(ApplicationState.CREATED, ApplicationEvent.VERIFY, ApplicationState.VERIFIED),
            new Transition(ApplicationState.CREATED, ApplicationEvent.DELETE, ApplicationState.DELETED),
            new Transition(ApplicationState.VERIFIED, ApplicationEvent.ACCEPT, ApplicationState.ACCEPTED),
            new Transition(ApplicationState.VERIFIED, ApplicationEvent.REJECT, ApplicationState.REJECTED),
            new Transition(ApplicationState.ACCEPTED, ApplicationEvent.PUBLISH, ApplicationState.PUBLISHED),
            new Transition(ApplicationState.ACCEPTED, ApplicationEvent.REJECT, ApplicationState.REJECTED)
    );

    private ApplicationStateMachine() {
    }

    public static ApplicationState transition(ApplicationState from, ApplicationEvent event) {
        return TRANSITIONS.stream()
                .filter(transition -> transition.from() == from && transition.event() == event)
                .map(Transition::to)
                .findFirst()
                .orElseThrow(() -> new InvalidTransitionException(from, event));
    }
}