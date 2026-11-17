package de.htwsaar.library.member;

import de.htwsaar.library.common.Exceptions;

public class MemberExceptions {

    public static class MemberNotFoundException extends Exceptions.NotFoundException {
        public MemberNotFoundException(String message) {
            super(message);
        }

        public MemberNotFoundException(Long id) {
            this("Member with id=" + id + " not found.");
        }
    }

    public static class MemberAlreadyExistsException extends Exceptions.ConflictException {
        public MemberAlreadyExistsException(String email) {
            super("member-already-exists","Member with email-address " + email + " already exists.");
        }
    }

    public static class MemberHasLoansException extends Exceptions.ConflictException {
        public MemberHasLoansException(Long id) {
            super("member-has-loans", "Member with id=" + id + " has loan history and cannot be deleted.");
        }
    }
}
