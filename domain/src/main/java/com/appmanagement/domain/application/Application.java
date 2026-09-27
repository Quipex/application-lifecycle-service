package com.appmanagement.domain.application;

import com.appmanagement.domain.application.events.ApplicationAcceptedEvent;
import com.appmanagement.domain.application.events.ApplicationBodyEditedEvent;
import com.appmanagement.domain.application.events.ApplicationCreatedEvent;
import com.appmanagement.domain.application.events.ApplicationDeletedEvent;
import com.appmanagement.domain.application.events.ApplicationPublishedEvent;
import com.appmanagement.domain.application.events.ApplicationRejectedEvent;
import com.appmanagement.domain.application.events.ApplicationVerifiedEvent;
import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.application.values.Reason;
import com.appmanagement.domain.shared.TransitionResult;
import com.appmanagement.domain.shared.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Optional;

@Getter
@Builder(toBuilder = true)
@ToString
public class Application {

    private final ApplicationId id;
    private final Name name;
    private final Body body;
    private final ApplicationState state;
    private final Optional<PublicationId> publicationId;
    private final Version version;
    private final Optional<Instant> createdAt;
    private final Optional<Instant> updatedAt;

    public Application(ApplicationId id, Name name, Body body, ApplicationState state,
                       Optional<PublicationId> publicationId, Version version,
                       Optional<Instant> createdAt, Optional<Instant> updatedAt) {
        if (id == null || name == null || body == null || state == null
                || publicationId == null || version == null || createdAt == null || updatedAt == null) {
            throw new DomainRuleViolationException("every application field must be present");
        }
        var published = state == ApplicationState.PUBLISHED;
        if (published != publicationId.isPresent()) {
            throw new DomainRuleViolationException("publicationId must be present if and only if state is PUBLISHED");
        }
        if (createdAt.isPresent() != updatedAt.isPresent()) {
            throw new DomainRuleViolationException("createdAt and updatedAt must both be present or both be empty");
        }
        if (createdAt.isPresent() && updatedAt.orElseThrow().isBefore(createdAt.orElseThrow())) {
            throw new DomainRuleViolationException("updatedAt must not be before createdAt");
        }
        this.id = id;
        this.name = name;
        this.body = body;
        this.state = state;
        this.publicationId = publicationId;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TransitionResult<Application, ApplicationCreatedEvent> create(Name name, Body body) {
        if (name == null || body == null) {
            throw new DomainRuleViolationException("name and body must be present");
        }
        var created = Application.builder()
                .id(ApplicationId.generate())
                .name(name)
                .body(body)
                .state(ApplicationState.CREATED)
                .publicationId(Optional.empty())
                .version(Version.initial())
                .createdAt(Optional.empty())
                .updatedAt(Optional.empty())
                .build();
        return new TransitionResult<>(created, new ApplicationCreatedEvent(created.getId()));
    }

    public TransitionResult<Application, ApplicationVerifiedEvent> verify() {
        var to = state.transition(ApplicationEvent.VERIFY);
        var next = toBuilder().state(to).build();
        return new TransitionResult<>(next, new ApplicationVerifiedEvent(id));
    }

    public TransitionResult<Application, ApplicationDeletedEvent> delete(Reason reason) {
        if (reason == null) {
            throw new DomainRuleViolationException("reason must be present");
        }
        var to = state.transition(ApplicationEvent.DELETE);
        var next = toBuilder().state(to).build();
        return new TransitionResult<>(next, new ApplicationDeletedEvent(id, reason));
    }

    public TransitionResult<Application, ApplicationAcceptedEvent> accept() {
        var to = state.transition(ApplicationEvent.ACCEPT);
        var next = toBuilder().state(to).build();
        return new TransitionResult<>(next, new ApplicationAcceptedEvent(id));
    }

    public TransitionResult<Application, ApplicationRejectedEvent> reject(Reason reason) {
        if (reason == null) {
            throw new DomainRuleViolationException("reason must be present");
        }
        var to = state.transition(ApplicationEvent.REJECT);
        var next = toBuilder().state(to).build();
        return new TransitionResult<>(next, new ApplicationRejectedEvent(id, state, reason));
    }

    public TransitionResult<Application, ApplicationPublishedEvent> publish(PublicationIdGenerator publicationIdGenerator) {
        if (publicationIdGenerator == null) {
            throw new DomainRuleViolationException("publicationIdGenerator must be present");
        }
        var to = state.transition(ApplicationEvent.PUBLISH);
        var publicationId = publicationIdGenerator.next();
        if (publicationId == null) {
            throw new DomainRuleViolationException("publicationIdGenerator must return a publicationId");
        }
        var next = toBuilder().state(to).publicationId(Optional.of(publicationId)).build();
        return new TransitionResult<>(next, new ApplicationPublishedEvent(id, publicationId));
    }

    public TransitionResult<Application, ApplicationBodyEditedEvent> updateBody(Body newBody) {
        if (newBody == null) {
            throw new DomainRuleViolationException("body must be present");
        }
        if (state != ApplicationState.CREATED && state != ApplicationState.VERIFIED) {
            throw new DomainRuleViolationException("body may only be edited while the application is in state "
                    + ApplicationState.CREATED + " or " + ApplicationState.VERIFIED + " but it is in state " + state);
        }
        var next = toBuilder().body(newBody).build();
        return new TransitionResult<>(next, new ApplicationBodyEditedEvent(id, state));
    }
}
