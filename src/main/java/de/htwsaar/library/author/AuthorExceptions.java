package de.htwsaar.library.author;

import de.htwsaar.library.common.Exceptions;

public class AuthorExceptions {

    public static class AuthorNotFoundException extends Exceptions.NotFoundException {
        public AuthorNotFoundException(String message) {
            super(message);
        }

        public AuthorNotFoundException(Long authorId) {
            super("Author with id=" + authorId + " not found.");
        }
    }

    public static class AuthorInUseException extends Exceptions.ConflictException {
        public AuthorInUseException(Long authorId) {
            super("author-in-use","Author with id=" + authorId + " has still connected records.");
        }
    }
}
