package de.htwsaar.library.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class Exceptions {

    public static class NotFoundException extends RuntimeException {

        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class ConflictException extends RuntimeException {
        private final String type;
        public ConflictException(String type, String message) {
            super(message);
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
