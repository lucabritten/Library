package de.htwsaar.library.auth;

import de.htwsaar.library.common.Exceptions;

public class AuthExceptions {

    public static class InvalidCredentialsException extends Exceptions.UnauthorizedException {
        public InvalidCredentialsException() {
            super("Invalid email or password.");
        }
    }
}
