package com.appmanagement.web.application;

public class InvalidNameFilterException extends RuntimeException {

    public InvalidNameFilterException() {
        super("name filter must be at most 100 characters long");
    }
}
