package com.appmanagement.web;

import com.appmanagement.application.application.ApplicationNotFoundException;
import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.domain.application.exception.DomainRuleViolationException;
import com.appmanagement.domain.application.exception.InvalidTransitionException;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.web.application.InvalidActorHeaderException;
import com.appmanagement.web.application.InvalidNameFilterException;
import com.appmanagement.web.application.InvalidPaginationException;
import com.appmanagement.web.application.UnknownStateFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationFailureMapsToBadRequestWithPerFieldDetails() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        bindingResult.addError(new FieldError("request", "body", "must not be blank"));
        var exception =
                new MethodArgumentNotValidException(sampleParameter(), bindingResult);

        var detail = handler.handleValidation(exception);

        assertEquals(400, detail.getStatus());
        assertEquals("Bad Request", detail.getTitle());
        assertEquals(List.of("name: must not be blank", "body: must not be blank"),
                detail.getProperties().get("errors"));
    }

    @Test
    void typeMismatchMapsToBadRequest() throws Exception {
        var exception = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "id", sampleParameter(), null);

        var detail = handler.handleTypeMismatch(exception);

        assertEquals(400, detail.getStatus());
        assertEquals("invalid value for parameter 'id'", detail.getDetail());
    }

    @Test
    void unknownStateFilterMapsToBadRequest() {
        var detail = handler.handleInvalidRequestInput(new UnknownStateFilterException("FOO"));

        assertEquals(400, detail.getStatus());
        assertEquals("unknown state filter value: FOO", detail.getDetail());
    }

    @Test
    void invalidActorHeaderMapsToBadRequest() {
        var detail = handler.handleInvalidRequestInput(new InvalidActorHeaderException("x".repeat(65)));

        assertEquals(400, detail.getStatus());
        assertEquals("Bad Request", detail.getTitle());
    }

    @Test
    void invalidPaginationMapsToBadRequest() {
        var detail = handler.handleInvalidRequestInput(new InvalidPaginationException(-1, 10));

        assertEquals(400, detail.getStatus());
        assertEquals("page must be at least 0, size must be between 1 and 100, and page offset must not exceed 100000"
                + " but was page=-1 size=10", detail.getDetail());
    }

    @Test
    void overlongNameFilterMapsToBadRequest() {
        var detail = handler.handleInvalidRequestInput(new InvalidNameFilterException());

        assertEquals(400, detail.getStatus());
        assertEquals("name filter must be at most 100 characters long", detail.getDetail());
    }

    @Test
    void applicationNotFoundMapsToNotFound() {
        var detail = handler.handleNotFound(new ApplicationNotFoundException(ApplicationId.generate()));

        assertEquals(404, detail.getStatus());
        assertEquals("Not Found", detail.getTitle());
    }

    @Test
    void invalidTransitionMapsToConflict() {
        var detail = handler.handleInvalidTransition(
                new InvalidTransitionException(ApplicationState.CREATED, ApplicationEvent.ACCEPT));

        assertEquals(409, detail.getStatus());
        assertEquals("Conflict", detail.getTitle());
        assertEquals("event ACCEPT is not allowed from state CREATED", detail.getDetail());
    }

    @Test
    void domainRuleViolationMapsToConflict() {
        var detail = handler.handleDomainRuleViolation(
                new DomainRuleViolationException("body may only be edited in CREATED or VERIFIED"));

        assertEquals(409, detail.getStatus());
        assertEquals("Conflict", detail.getTitle());
    }

    @Test
    void optimisticLockingMapsToConflict() {
        var detail = handler.handleOptimisticLocking(new OptimisticLockingFailureException("stale"));

        assertEquals(409, detail.getStatus());
        assertEquals("the application was modified concurrently, please retry", detail.getDetail());
    }

    private MethodParameter sampleParameter() throws Exception {
        var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleTarget", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void sampleTarget(String sample) {
    }
}