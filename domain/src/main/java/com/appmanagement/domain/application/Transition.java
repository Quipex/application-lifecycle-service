package com.appmanagement.domain.application;

public record Transition(ApplicationState from, ApplicationEvent event, ApplicationState to) {
}