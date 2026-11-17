package de.htwsaar.library.common;


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

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
