package com.appmanagement.domain.application.exception;

import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import lombok.Getter;

@Getter
public class InvalidTransitionException extends RuntimeException {

    private final ApplicationState from;
    private final ApplicationEvent event;

    public InvalidTransitionException(ApplicationState from, ApplicationEvent event) {
        super("event " + event + " is not allowed from state " + from);
        this.from = from;
        this.event = event;
    }
}