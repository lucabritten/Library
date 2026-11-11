package de.htwsaar.library.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ApiExceptionHandler.class);


    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLock(OptimisticLockingFailureException ex) {
        logger.info("Optimistic lock conflict: {}", ex.getMessage()); // Details nur im LOG

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was modified concurrently. Please reload and try again.");
        pd.setType(URI.create("https://library.example/problems/concurrent-modification"));
        pd.setTitle("Concurrent modification");
        return pd;
    }


    @ExceptionHandler(Exceptions.ConflictException.class)
    ProblemDetail authorInUse(Exceptions.ConflictException ex) {
        logger.info("CONFLICT-EXCEPTION: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        pd.setType(URI.create("https://library.example/problems/" + ex.getType()));
        pd.setTitle("Conflict with requested resource");

        return pd;
    }

    @ExceptionHandler(Exceptions.NotFoundException.class)
    ProblemDetail notFound(Exceptions.NotFoundException ex) {
        logger.info("NOT-FOUND-EXCEPTION: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        pd.setType(URI.create("https://library.example/problems/not-found"));
        pd.setTitle("Requested resource not found");

        return pd;
    }


    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleBadSort(PropertyReferenceException ex) {
        logger.info("PROPERY-REFERENCY-EXCEPTION: Invalid sort property: {}", ex.getPropertyName());

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid sort property: " + ex.getPropertyName());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setTitle("Validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "invalid"),
                        (a, b) -> a)));
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpectedError(Exception ex) {
        logger.error("Unexpected error: ", ex);

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }
}
