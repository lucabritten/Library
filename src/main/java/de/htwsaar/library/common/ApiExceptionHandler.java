package de.htwsaar.library.common;

import de.htwsaar.library.author.AuthorExceptions;
import de.htwsaar.library.book.BookExceptions;
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


    @ExceptionHandler({BookExceptions.DuplicateIsbnException.class, OptimisticLockingFailureException.class})
    ProblemDetail duplicateIsbn(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Konflikt mit dem aktuellen Zustand");

        pd.setDetail(ex instanceof BookExceptions.DuplicateIsbnException ? ex.getMessage()
                : "Der Datensatz wurde gerade von jemand anderem geändert. Bitte neu laden.");
        return pd;
    }


    @ExceptionHandler(AuthorExceptions.AuthorInUseException.class)
    ProblemDetail authorInUse(AuthorExceptions.AuthorInUseException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        pd.setType(URI.create("https://library.example/problems/author-in-use"));
        pd.setTitle("Author still in use");

        return pd;
    }

    @ExceptionHandler(Exceptions.NotFoundException.class)
    ProblemDetail notFound(Exceptions.NotFoundException ex) {
        logger.info("NOT-FOUND-EXCEPTION: {}", ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
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
