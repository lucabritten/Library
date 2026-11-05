package de.htwsaar.library.book;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BookExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class BookNotFoundException extends RuntimeException {

        public BookNotFoundException(String message) {
            super(message);
        }

        public BookNotFoundException(Long id) {
            super("Book with ID= " + id + " not found.");
        }
    }
}
