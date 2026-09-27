package com.appmanagement.web.application;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(int page, int size) {
        super("page must be at least 0, size must be between 1 and 100, and page offset must not exceed 100000"
                + " but was page=" + page + " size=" + size);
    }
}
