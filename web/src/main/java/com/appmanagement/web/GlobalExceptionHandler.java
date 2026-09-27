package com.appmanagement.web;

import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import com.appmanagement.domain.application.exception.InvalidTransitionException;
import com.appmanagement.web.application.InvalidActorHeaderException;
import com.appmanagement.web.application.InvalidNameFilterException;
import com.appmanagement.web.application.InvalidPaginationException;
import com.appmanagement.web.application.UnknownStateFilterException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var detail = problemDetail(HttpStatus.BAD_REQUEST, "request validation failed");
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return detail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problemDetail(HttpStatus.BAD_REQUEST,
                "invalid value for parameter '" + exception.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return problemDetail(HttpStatus.BAD_REQUEST, "request body is missing or not readable as JSON");
    }

    @ExceptionHandler({UnknownStateFilterException.class, InvalidActorHeaderException.class,
            InvalidNameFilterException.class, InvalidPaginationException.class})
    public ProblemDetail handleInvalidRequestInput(RuntimeException exception) {
        return problemDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ProblemDetail handleNotFound(ApplicationNotFoundException exception) {
        return problemDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException exception) {
        return problemDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ProblemDetail handleDomainRuleViolation(DomainRuleViolationException exception) {
        return problemDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException exception) {
        return problemDetail(HttpStatus.CONFLICT, "the application was modified concurrently, please retry");
    }

    private ProblemDetail problemDetail(HttpStatus status, String message) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return detail;
    }
}