package de.htwsaar.library.loan;

import de.htwsaar.library.TestData;
import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.copy.BookCopyExceptions;
import de.htwsaar.library.copy.BookCopyService;
import de.htwsaar.library.copy.CopyStatus;
import de.htwsaar.library.member.Member;
import de.htwsaar.library.member.MemberExceptions;
import de.htwsaar.library.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepo loanRepo;
    @Mock MemberService memberService;
    @Mock BookCopyService bookCopyService;

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 10);
    private static final Clock CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant(), ZoneId.of("Europe/Berlin"));

    private LoanService service;
    private Member member;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepo, new LoanProperties(5, Period.ofDays(14)),
                memberService, bookCopyService, CLOCK);
        member = TestData.aMember();
        copy = TestData.aCopy();   // AVAILABLE
    }

    private void memberAndCopyExist() {
        when(memberService.require(1L)).thenReturn(member);
        when(bookCopyService.require("BC-000001")).thenReturn(copy);
    }

    @Test
    void require_noLoanWithId_throwsNotFound() {
        when(loanRepo.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.require(123L))
                .isInstanceOf(LoanExceptions.LoanNotFoundException.class);
    }

    @Test
    void lend_belowLimit_createsLoanDueInTwoWeeks() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(4L);

        LoanDtos.LoanResponse response = service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001"));

        assertThat(response.loanedAt()).isEqualTo(TODAY);
        assertThat(response.dueAt()).isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void lend_atLimit_throwsAndLeavesCopyAvailable() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001")))
                .isInstanceOf(LoanExceptions.LoanLimitExceededException.class);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_setDueAt_isInLoanPeriodDays() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        LoanDtos.LoanResponse response = service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001"));

        assertThat(response.dueAt()).isEqualTo(TODAY.plusDays(14));
    }

    @Test
    void lend_setsCopyStatusToLoaned() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001"));

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void lend_notAvailableCopy_throwsNotAvailable() {
        memberAndCopyExist();
        copy.markLoaned();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001")))
                .isInstanceOf(BookCopyExceptions.CopyNotAvailableException.class);
        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_unknownMember_throwsAndSavesNothing() {
        when(memberService.require(99L))
                .thenThrow(new MemberExceptions.MemberNotFoundException(99L));

        assertThatThrownBy(() -> service.lend(new LoanDtos.CreateLoanRequest(99L, "BC-000001")))
                .isInstanceOf(MemberExceptions.MemberNotFoundException.class);

        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_copyLoanedWhileSaving_throws() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);
        when(loanRepo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.lend(new LoanDtos.CreateLoanRequest(1L, "BC-000001")))
                .isInstanceOf(BookCopyExceptions.CopyNotAvailableException.class);
    }

    @Test
    void returnLoan_returnedBook_isAvailable() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(1), TODAY.plusDays(1));

        when(loanRepo.findById(any()))
                .thenReturn(Optional.of(loan));

        service.returnLoan(1L);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void returnLoan_responseContainsDaysOverdue() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(5));

        when(loanRepo.findById(any()))
                .thenReturn(Optional.of(loan));

        LoanDtos.LoanResponse response = service.returnLoan(1L);

        assertThat(response.daysOverdue()).isEqualTo(5);
    }

    @Test
    void returnLoan_setsReturnedAtToady() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(5));

        when(loanRepo.findById(any()))
                .thenReturn(Optional.of(loan));

        LoanDtos.LoanResponse response = service.returnLoan(1L);

        assertThat(response.returnedAt()).isEqualTo(TODAY);
    }

    @Test
    void returnLoan_alreadyReturnedLoan_throws() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(5));
        when(loanRepo.findById(any()))
                .thenReturn(Optional.of(loan));

        service.returnLoan(1L);

       assertThatThrownBy(() -> service.returnLoan(1L))
               .isInstanceOf(LoanExceptions.CopyAlreadyReturnedException.class);
    }

}
