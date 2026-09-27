package com.appmanagement.application;

import com.appmanagement.application.shared.Actor;
import com.appmanagement.domain.application.events.TransitionEvent;

public interface AuditLog {

    void append(Actor actor, TransitionEvent event);
}