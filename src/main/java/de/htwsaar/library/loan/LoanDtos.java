package de.htwsaar.library.loan;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class LoanDtos {

    public record CreateLoanRequest(
            @NotNull
            Long memberId,

            @NotBlank
            String inventoryNumber
    ) {}

    public record LoanResponse(
        Long id,
        Long memberId,
        String inventoryNumber,
        LocalDate loanedAt,
        LocalDate dueAt,
        LocalDate returnedAt,
        Long daysOverdue
    ) {

        public static LoanResponse from(Loan loan, LocalDate today) {
            return new LoanResponse(
                    loan.getId(),
                    loan.getMember().getId(),
                    loan.getCopy().getInventoryNumber(),
                    loan.getLoanedAt(),
                    loan.getDueAt(),
                    loan.getReturnedAt(),
                    loan.daysOverdue(today)
            );
        }
    }
}
