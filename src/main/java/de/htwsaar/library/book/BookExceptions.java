package de.htwsaar.library.book;


import de.htwsaar.library.common.Exceptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BookExceptions {

    public static class BookNotFoundException extends Exceptions.NotFoundException {
        public BookNotFoundException(String message) {
            super(message);
        }

        public BookNotFoundException(Long id) {
            super("Book with ID= " + id + " not found.");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateIsbnException extends RuntimeException {
        public DuplicateIsbnException(String isbn) {
            super("ISBN " + isbn + " already exists in database.");
        }
    }
}
