package com.appmanagement.web.application;

public class UnknownStateFilterException extends RuntimeException {

    public UnknownStateFilterException(String value) {
        super("unknown state filter value: " + value);
    }
}