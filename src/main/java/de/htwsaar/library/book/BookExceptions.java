package de.htwsaar.library.book;


import de.htwsaar.library.common.Exceptions;


public class BookExceptions {

    public static class BookNotFoundException extends Exceptions.NotFoundException {
        public BookNotFoundException(String message) {
            super(message);
        }

        public BookNotFoundException(Long id) {
            super("Book with ID= " + id + " not found.");
        }
    }

    public static class DuplicateIsbnException extends Exceptions.ConflictException {
        public DuplicateIsbnException(String isbn) {
            super("duplicate-isbn","ISBN " + isbn + " already exists in database.");
        }
    }

    public static class BookInUseException extends Exceptions.ConflictException {
        public BookInUseException(Long id) {
            super("book-in-use", "Book with id=" + id + " cannot be deleted, as its still in use.");
        }
    }
}
