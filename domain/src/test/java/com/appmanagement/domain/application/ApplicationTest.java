package com.appmanagement.domain.application;

import com.appmanagement.domain.application.events.ApplicationAcceptedEvent;
import com.appmanagement.domain.application.events.ApplicationBodyEditedEvent;
import com.appmanagement.domain.application.events.ApplicationCreatedEvent;
import com.appmanagement.domain.application.events.ApplicationDeletedEvent;
import com.appmanagement.domain.application.events.ApplicationPublishedEvent;
import com.appmanagement.domain.application.events.ApplicationRejectedEvent;
import com.appmanagement.domain.application.events.ApplicationVerifiedEvent;
import com.appmanagement.domain.application.events.TransitionEvent;
import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import com.appmanagement.domain.application.exception.InvalidTransitionException;
import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.PublicationId;
import com.appmanagement.domain.application.values.Reason;
import com.appmanagement.domain.shared.Version;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Name NAME = new Name("Loan request");
    private static final Body BODY = new Body("Please approve the loan");
    private static final Reason REASON = new Reason("Not needed anymore");

    private Application created() {
        return Application.create(NAME, BODY).next();
    }

    private Application verified() {
        return created().verify().next();
    }

    private Application accepted() {
        return verified().accept().next();
    }

    private Application published() {
        return accepted().publish(mockGenerator(777)).next();
    }

    private PublicationIdGenerator mockGenerator(long value) {
        var generator = mock(PublicationIdGenerator.class);
        when(generator.next()).thenReturn(new PublicationId(value));
        return generator;
    }

    @Test
    void createBuildsApplicationDirectlyInCreatedState() {
        var result = Application.create(NAME, BODY);

        var application = result.next();
        assertEquals(ApplicationState.CREATED, application.getState());
        assertTrue(application.getPublicationId().isEmpty());
        assertEquals(Version.initial(), application.getVersion());
        assertInstanceOf(ApplicationCreatedEvent.class, result.event());
    }

    @Test
    void createLeavesTimestampsEmptyUntilPersistence() {
        var application = created();

        assertEquals(Optional.empty(), application.getCreatedAt());
        assertEquals(Optional.empty(), application.getUpdatedAt());
    }

    @Test
    void createdEventCarriesCreationSemantics() {
        var event = Application.create(NAME, BODY).event();

        assertEquals(ApplicationEvent.CREATE, event.event());
        assertEquals(Optional.empty(), event.stateFrom());
        assertEquals(ApplicationState.CREATED, event.stateTo());
        assertEquals(Optional.empty(), event.reason());
        assertEquals(TransitionEvent.AGGREGATE_TYPE, event.aggregateType());
    }

    @Test
    void createGeneratesUniqueIds() {
        var first = created().getId();
        var second = created().getId();

        assertNotSame(first, second);
        assertNotSame(first.value(), second.value());
    }

    @Test
    void publicMutationMethodsRejectNullArgumentsWithDomainException() {
        assertThrows(DomainRuleViolationException.class, () -> Application.create(null, BODY));
        assertThrows(DomainRuleViolationException.class, () -> Application.create(NAME, null));
        assertThrows(DomainRuleViolationException.class, () -> created().delete(null));
        assertThrows(DomainRuleViolationException.class, () -> verified().reject(null));
        assertThrows(DomainRuleViolationException.class, () -> accepted().publish(null));
        assertThrows(DomainRuleViolationException.class, () -> accepted().publish(() -> null));
        assertThrows(DomainRuleViolationException.class, () -> created().updateBody(null));
    }

    @Test
    void verifyReturnsVerifiedCopyAndLeavesSourceInCreated() {
        var source = created();

        var result = source.verify();

        assertEquals(ApplicationState.CREATED, source.getState());
        assertEquals(ApplicationState.VERIFIED, result.next().getState());
        assertNotSame(source, result.next());
        assertEquals(source.getId(), result.next().getId());
        assertEquals(Optional.empty(), result.next().getCreatedAt());
        assertEquals(Optional.empty(), result.next().getUpdatedAt());
        var event = assertInstanceOf(ApplicationVerifiedEvent.class, result.event());
        assertEquals(source.getId(), event.aggregateId());
        assertEquals(Optional.of(ApplicationState.CREATED), event.stateFrom());
        assertEquals(ApplicationState.VERIFIED, event.stateTo());
    }

    @Test
    void verifyTwiceIsRejected() {
        var verified = verified();

        var exception = assertThrows(InvalidTransitionException.class, verified::verify);
        assertEquals(ApplicationState.VERIFIED, exception.getFrom());
        assertEquals(ApplicationEvent.VERIFY, exception.getEvent());
    }

    @Test
    void deleteMovesCreatedToDeletedAndCarriesReason() {
        var source = created();

        var result = source.delete(REASON);

        assertEquals(ApplicationState.DELETED, result.next().getState());
        assertTrue(result.next().getPublicationId().isEmpty());
        assertEquals(ApplicationState.CREATED, source.getState());
        var event = assertInstanceOf(ApplicationDeletedEvent.class, result.event());
        assertEquals(Optional.of(REASON), event.reason());
        assertEquals(Optional.of(ApplicationState.CREATED), event.stateFrom());
        assertEquals(ApplicationState.DELETED, event.stateTo());
    }

    @Test
    void deleteFromPublishedIsRejected() {
        var published = published();

        assertThrows(InvalidTransitionException.class, () -> published.delete(REASON));
    }

    @Test
    void acceptMovesVerifiedToAccepted() {
        var source = verified();

        var result = source.accept();

        assertEquals(ApplicationState.ACCEPTED, result.next().getState());
        assertEquals(ApplicationState.VERIFIED, source.getState());
        var event = assertInstanceOf(ApplicationAcceptedEvent.class, result.event());
        assertEquals(Optional.of(ApplicationState.VERIFIED), event.stateFrom());
        assertEquals(ApplicationState.ACCEPTED, event.stateTo());
    }

    @Test
    void acceptFromCreatedIsRejected() {
        assertThrows(InvalidTransitionException.class, () -> created().accept());
    }

    @Test
    void rejectIsReachableFromVerified() {
        var source = verified();

        var result = source.reject(REASON);

        assertEquals(ApplicationState.REJECTED, result.next().getState());
        var event = assertInstanceOf(ApplicationRejectedEvent.class, result.event());
        assertEquals(Optional.of(ApplicationState.VERIFIED), event.stateFrom());
        assertEquals(ApplicationState.REJECTED, event.stateTo());
        assertEquals(Optional.of(REASON), event.reason());
    }

    @Test
    void rejectIsReachableFromAccepted() {
        var source = accepted();

        var result = source.reject(REASON);

        assertEquals(ApplicationState.REJECTED, result.next().getState());
        var event = assertInstanceOf(ApplicationRejectedEvent.class, result.event());
        assertEquals(Optional.of(ApplicationState.ACCEPTED), event.stateFrom());
        assertEquals(ApplicationState.REJECTED, event.stateTo());
        assertEquals(Optional.of(REASON), event.reason());
    }

    @Test
    void rejectFromCreatedIsRejected() {
        assertThrows(InvalidTransitionException.class, () -> created().reject(REASON));
    }

    @Test
    void publishAssignsPublicationIdFromGenerator() {
        var source = accepted();
        var generator = mockGenerator(42);

        var result = source.publish(generator);

        assertEquals(ApplicationState.PUBLISHED, result.next().getState());
        assertEquals(Optional.of(new PublicationId(42)), result.next().getPublicationId());
        assertTrue(source.getPublicationId().isEmpty());
        var event = assertInstanceOf(ApplicationPublishedEvent.class, result.event());
        assertEquals(new PublicationId(42), event.publicationId());
        assertEquals(Optional.of(ApplicationState.ACCEPTED), event.stateFrom());
        assertEquals(ApplicationState.PUBLISHED, event.stateTo());
    }

    @Test
    void publicationIdIsEmptyUntilPublish() {
        assertTrue(created().getPublicationId().isEmpty());
        assertTrue(verified().getPublicationId().isEmpty());
        assertTrue(accepted().getPublicationId().isEmpty());
    }

    @Test
    void publishTwiceIsRejected() {
        var published = published();

        var exception =
                assertThrows(InvalidTransitionException.class, () -> published.publish(mockGenerator(1)));
        assertEquals(ApplicationState.PUBLISHED, exception.getFrom());
        assertEquals(ApplicationEvent.PUBLISH, exception.getEvent());
    }

    @Test
    void publishFromVerifiedIsRejected() {
        assertThrows(InvalidTransitionException.class, () -> verified().publish(mockGenerator(1)));
    }

    @Test
    void updateBodyIsAllowedInCreated() {
        var source = created();

        var result = source.updateBody(new Body("Updated body"));

        assertEquals(new Body("Updated body"), result.next().getBody());
        assertEquals(ApplicationState.CREATED, result.next().getState());
        assertEquals(BODY, source.getBody());
        assertEquals(NAME, result.next().getName());
        var event = assertInstanceOf(ApplicationBodyEditedEvent.class, result.event());
        assertEquals(Optional.of(ApplicationState.CREATED), event.stateFrom());
        assertEquals(ApplicationState.CREATED, event.stateTo());
        assertEquals(event.stateFrom(), Optional.of(event.stateTo()));
    }

    @Test
    void updateBodyIsAllowedInVerified() {
        var source = verified();

        var result = source.updateBody(new Body("Updated body"));

        assertEquals(new Body("Updated body"), result.next().getBody());
        assertEquals(ApplicationState.VERIFIED, result.next().getState());
    }

    @Test
    void updateBodyIsRejectedInAccepted() {
        var exception =
                assertThrows(DomainRuleViolationException.class, () -> accepted().updateBody(new Body("Updated body")));
        assertTrue(exception.getMessage().contains("ACCEPTED"));
    }

    @Test
    void updateBodyIsRejectedInPublished() {
        assertThrows(DomainRuleViolationException.class, () -> published().updateBody(new Body("Updated body")));
    }

    @Test
    void updateBodyIsRejectedInDeleted() {
        assertThrows(DomainRuleViolationException.class,
                () -> created().delete(REASON).next().updateBody(new Body("Updated body")));
    }

    @Test
    void updateBodyIsRejectedInRejected() {
        assertThrows(DomainRuleViolationException.class,
                () -> verified().reject(REASON).next().updateBody(new Body("Updated body")));
    }

    @Test
    void builderRejectsMissingFields() {
        assertThrows(DomainRuleViolationException.class, () -> Application.builder().build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().id(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().name(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().body(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().state(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().publicationId(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().version(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().createdAt(null).build());
        assertThrows(DomainRuleViolationException.class, () -> validBuilder().updatedAt(null).build());
    }

    @Test
    void builderRejectsPublicationIdOutsidePublishedState() {
        assertThrows(DomainRuleViolationException.class,
                () -> validBuilder().publicationId(Optional.of(new PublicationId(1))).build());
    }

    @Test
    void builderRejectsPublishedStateWithoutPublicationId() {
        assertThrows(DomainRuleViolationException.class,
                () -> validBuilder().state(ApplicationState.PUBLISHED).build());
    }

    @Test
    void builderRejectsInvalidTimestampOptionals() {
        assertThrows(DomainRuleViolationException.class,
                () -> validBuilder().updatedAt(Optional.of(NOW.minusSeconds(1))).build());
        assertThrows(DomainRuleViolationException.class,
                () -> validBuilder().createdAt(Optional.empty()).build());
        assertThrows(DomainRuleViolationException.class,
                () -> validBuilder().updatedAt(Optional.empty()).build());
    }

    @Test
    void toBuilderDelegatesToTheValidatingConstructor() {
        var source = created();

        assertThrows(DomainRuleViolationException.class,
                () -> source.toBuilder().state(ApplicationState.PUBLISHED).build());
        assertThrows(DomainRuleViolationException.class,
                () -> source.toBuilder().publicationId(Optional.of(new PublicationId(1))).build());
        assertThrows(DomainRuleViolationException.class,
                () -> source.toBuilder().updatedAt(Optional.of(NOW.minusSeconds(1))).build());
        assertThrows(DomainRuleViolationException.class, () -> source.toBuilder().name(null).build());
    }

    @Test
    void builderAssemblesValidApplication() {
        var application = validBuilder().build();

        assertEquals(ApplicationState.CREATED, application.getState());
        assertEquals(Optional.of(NOW), application.getCreatedAt());
        assertEquals(Version.initial(), application.getVersion());
    }

    @Test
    void transitionCopiesCarryVersionAndTimestampOptionalsUnchanged() {
        var source = created().toBuilder()
                .createdAt(Optional.of(NOW))
                .updatedAt(Optional.of(NOW))
                .build();

        var verified = source.verify().next();
        var deleted = source.delete(REASON).next();
        var edited = source.updateBody(new Body("Edited body")).next();
        var accepted = verified.accept().next();
        var rejected = verified.reject(REASON).next();
        var published = accepted.publish(mockGenerator(5)).next();

        assertEquals(Version.initial(), verified.getVersion());
        assertEquals(Version.initial(), accepted.getVersion());
        assertEquals(Version.initial(), published.getVersion());
        for (var copy : new Application[]{verified, deleted, edited, accepted, rejected, published}) {
            assertEquals(Optional.of(NOW), copy.getCreatedAt());
            assertEquals(Optional.of(NOW), copy.getUpdatedAt());
        }
    }

    private Application.ApplicationBuilder validBuilder() {
        return Application.builder()
                .id(ApplicationId.generate())
                .name(NAME)
                .body(BODY)
                .state(ApplicationState.CREATED)
                .publicationId(Optional.empty())
                .version(Version.initial())
                .createdAt(Optional.of(NOW))
                .updatedAt(Optional.of(NOW));
    }
}