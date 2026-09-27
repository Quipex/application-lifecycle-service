package com.appmanagement.web.application;

public class InvalidActorHeaderException extends RuntimeException {

    public InvalidActorHeaderException(String value) {
        super("X-Actor-Id must not be blank and must be at most 64 characters long but was: '" + value + "'");
    }
}