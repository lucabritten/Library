package de.htwsaar.library.loan;

import de.htwsaar.library.common.Exceptions;

public class LoanExceptions {

    public static class CopyAlreadyReturnedException extends Exceptions.ConflictException {
        public CopyAlreadyReturnedException(Long loanId) {
            super("copy-already-returned", "Loan with id=" + loanId + " cannot be returned, as it was already done.");
        }
    }

    public static class LoanLimitExceededException extends Exceptions.ConflictException {
        public LoanLimitExceededException (Long memberId, Long openLoans) {
            super("loan-limit-exceeded","Member with id=" + memberId + " exceeded his open-loans limit with " + openLoans + " currently open loans.");
        }
    }

    public static class LoanNotFoundException extends Exceptions.NotFoundException {
        public LoanNotFoundException(Long loanId) {
            super("Loan with id=" + loanId + " not found.");
        }
    }
}
