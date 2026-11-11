package de.htwsaar.library.loan;

import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.copy.BookCopyExceptions;
import de.htwsaar.library.copy.BookCopyService;
import de.htwsaar.library.member.Member;
import de.htwsaar.library.member.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepo loanRepo;
    private final LoanProperties loanProperties;

    private final MemberService memberService;
    private final BookCopyService bookCopyService;

    private final Clock clock;

    private final static Logger logger =
            LoggerFactory.getLogger(LoanService.class);

    public LoanService(LoanRepo loanRepo, LoanProperties loanProperties,MemberService memberService, BookCopyService bookCopyService, Clock clock) {
        this.loanRepo = loanRepo;
        this.loanProperties = loanProperties;

        this.memberService = memberService;
        this.bookCopyService = bookCopyService;

        this.clock = clock;
    }

    public Loan require(Long id) {
        return loanRepo.findById(id)
                .orElseThrow(() -> {
                    logger.info("Loan with id={} not found", id);
                    return new LoanExceptions.LoanNotFoundException(id);
                });
    }

    public LoanDtos.LoanResponse findById(Long id) {
        LocalDate now = LocalDate.now(clock);
        return LoanDtos.LoanResponse.from(require(id), now);
    }

    @Transactional
    public LoanDtos.LoanResponse lend(LoanDtos.CreateLoanRequest req) {
        Long memberId = req.memberId();
        String inventoryNumber = req.inventoryNumber();

        Member member = memberService.require(memberId);
        BookCopy copy = bookCopyService.require(inventoryNumber);

        int maxLoans = loanProperties.maxActiveLoans();
        long openLoans = loanRepo.countByMemberIdAndReturnedAtIsNull(memberId);

        if (maxLoans <= openLoans)
            throw new LoanExceptions.LoanLimitExceededException(memberId, openLoans);

        copy.markLoaned();
        LocalDate now = LocalDate.now(clock);
        LocalDate dueAt = now.plus(loanProperties.loanPeriod());

        Loan saved = new Loan(member, copy, now, dueAt);
        try {

            loanRepo.saveAndFlush(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BookCopyExceptions.CopyNotAvailableException("Book not currently not available.");
        }

        return LoanDtos.LoanResponse.from(saved, now);
    }

    @Transactional
    public LoanDtos.LoanResponse returnLoan(Long loanId) {
        Loan loan = require(loanId);
        LocalDate now = LocalDate.now(clock);

        loan.returnBook(now);
        loan.getCopy().markReturned();

        long daysOverdue = loan.daysOverdue(now);

        return LoanDtos.LoanResponse.from(loan, now);

    }
}
