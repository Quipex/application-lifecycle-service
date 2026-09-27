package com.appmanagement.domain.shared;

public record TransitionResult<A, E>(A next, E event) {
}
