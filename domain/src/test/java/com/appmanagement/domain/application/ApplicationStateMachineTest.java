package com.appmanagement.domain.application;

import com.appmanagement.domain.application.events.TransitionEvent;
import com.appmanagement.domain.application.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStateMachineTest {

    private static final Map<String, ApplicationState> DIAGRAM_EDGES = diagramEdges();

    private static Map<String, ApplicationState> diagramEdges() {
        Map<String, ApplicationState> edges = new HashMap<>();
        edges.put(edgeKey(ApplicationState.CREATED, ApplicationEvent.VERIFY), ApplicationState.VERIFIED);
        edges.put(edgeKey(ApplicationState.CREATED, ApplicationEvent.DELETE), ApplicationState.DELETED);
        edges.put(edgeKey(ApplicationState.VERIFIED, ApplicationEvent.ACCEPT), ApplicationState.ACCEPTED);
        edges.put(edgeKey(ApplicationState.VERIFIED, ApplicationEvent.REJECT), ApplicationState.REJECTED);
        edges.put(edgeKey(ApplicationState.ACCEPTED, ApplicationEvent.PUBLISH), ApplicationState.PUBLISHED);
        edges.put(edgeKey(ApplicationState.ACCEPTED, ApplicationEvent.REJECT), ApplicationState.REJECTED);
        return Map.copyOf(edges);
    }

    private static String edgeKey(ApplicationState from, ApplicationEvent event) {
        return from.name() + "|" + event.name();
    }

    @Test
    void transitionTableContainsExactlyTheSixPostCreationEdges() {
        var legalEdgeCount = 0;

        for (var from : ApplicationState.values()) {
            for (var event : ApplicationEvent.values()) {
                var key = edgeKey(from, event);
                if (DIAGRAM_EDGES.containsKey(key)) {
                    legalEdgeCount++;
                } else {
                    assertThrows(InvalidTransitionException.class,
                            () -> ApplicationStateMachine.transition(from, event), key);
                }
            }
        }

        assertEquals(6, legalEdgeCount);
    }

    @Test
    void transitionTableMatchesTheDiagram() {
        DIAGRAM_EDGES.forEach((key, expectedTo) -> {
            var parts = key.split("\\|");
            var from = ApplicationState.valueOf(parts[0]);
            var event = ApplicationEvent.valueOf(parts[1]);
            assertEquals(expectedTo, ApplicationStateMachine.transition(from, event), key);
        });
    }

    @Test
    void createIsNotATableEntry() {
        for (var from : ApplicationState.values()) {
            assertThrows(InvalidTransitionException.class,
                    () -> ApplicationStateMachine.transition(from, ApplicationEvent.CREATE), from.name());
        }
    }

    @Test
    void editIsNotATableEntry() {
        for (var from : ApplicationState.values()) {
            assertThrows(InvalidTransitionException.class,
                    () -> ApplicationStateMachine.transition(from, ApplicationEvent.EDIT), from.name());
        }
    }

    @Test
    void stateDelegatesTheCheckToTheMachine() {
        assertEquals(ApplicationState.VERIFIED, ApplicationState.CREATED.transition(ApplicationEvent.VERIFY));
        assertThrows(InvalidTransitionException.class,
                () -> ApplicationState.CREATED.transition(ApplicationEvent.ACCEPT));
    }

    @Test
    void eventHierarchyIsSealedWithExactlySevenEventTypes() {
        var eventNames = Arrays.stream(TransitionEvent.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "ApplicationCreatedEvent",
                "ApplicationVerifiedEvent",
                "ApplicationDeletedEvent",
                "ApplicationAcceptedEvent",
                "ApplicationRejectedEvent",
                "ApplicationPublishedEvent",
                "ApplicationBodyEditedEvent"), eventNames);
    }

    @Test
    void illegalTransitionReportsBothStates() {
        var exception =
                assertThrows(InvalidTransitionException.class,
                        () -> ApplicationStateMachine.transition(ApplicationState.CREATED, ApplicationEvent.PUBLISH));

        assertEquals(ApplicationState.CREATED, exception.getFrom());
        assertEquals(ApplicationEvent.PUBLISH, exception.getEvent());
        assertTrue(exception.getMessage().contains("CREATED"));
        assertTrue(exception.getMessage().contains("PUBLISH"));
    }
}