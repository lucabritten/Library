package de.htwsaar.library.loan;

import de.htwsaar.library.TestData;
import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.copy.BookCopyExceptions;
import de.htwsaar.library.copy.BookCopyService;
import de.htwsaar.library.copy.CopyStatus;
import de.htwsaar.library.member.Member;
import de.htwsaar.library.member.MemberExceptions;
import de.htwsaar.library.member.MemberService;
import de.htwsaar.library.member.Role;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
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
    private Loan sampleLoan;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new LoanService(loanRepo, new LoanProperties(5, Period.ofDays(14)),
                memberService, bookCopyService, CLOCK, new LoanMetrics(registry, loanRepo));
        member = TestData.aMember();
        copy = TestData.aCopy();// AVAILABLE
        sampleLoan = new Loan(member, copy,  TODAY.minusDays(1), TODAY.plusWeeks(3));
    }

    private void memberAndCopyExist() {
        when(memberService.require(1L)).thenReturn(member);
        when(bookCopyService.require("BC-000001")).thenReturn(copy);
    }

    @Test
    void findById_noLoanWithId_throwsNotFound() {
        when(loanRepo.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(123L, 999L, true))
                .isInstanceOf(LoanExceptions.LoanNotFoundException.class);
    }

    @Test
    void findById_asAdmin_usesFindByIdWithoutOwner() {
        when(loanRepo.findById(1L))
                .thenReturn(Optional.of(sampleLoan));

        service.findById(1L, 1L, true);

        verify(loanRepo, never()).findByIdAndMemberId(1L, 1L);
    }

    @Test
    void lend_belowLimit_createsLoanDueInTwoWeeks() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(4L);

        LoanDtos.LoanResponse response = service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001"));

        assertThat(response.loanedAt()).isEqualTo(TODAY);
        assertThat(response.dueAt()).isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void lend_atLimit_throwsAndLeavesCopyAvailable() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001")))
                .isInstanceOf(LoanExceptions.LoanLimitExceededException.class);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_setDueAt_isInLoanPeriodDays() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        LoanDtos.LoanResponse response = service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001"));

        assertThat(response.dueAt()).isEqualTo(TODAY.plusDays(14));
    }

    @Test
    void lend_setsCopyStatusToLoaned() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001"));

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void lend_notAvailableCopy_throwsNotAvailable() {
        memberAndCopyExist();
        copy.markLoaned();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001")))
                .isInstanceOf(BookCopyExceptions.CopyNotAvailableException.class);
        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_unknownMember_throwsAndSavesNothing() {
        when(memberService.require(99L))
                .thenThrow(new MemberExceptions.MemberNotFoundException(99L));

        assertThatThrownBy(() -> service.lend(99L,new LoanDtos.CreateLoanRequest( "BC-000001")))
                .isInstanceOf(MemberExceptions.MemberNotFoundException.class);

        verify(loanRepo, never()).saveAndFlush(any());
    }

    @Test
    void lend_copyLoanedWhileSaving_throws() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);
        when(loanRepo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001")))
                .isInstanceOf(BookCopyExceptions.CopyNotAvailableException.class);
    }

    @Test
    void lend_succeededLoan_isCountedInMetric() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(1L);

        service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001"));

        assertThat(registry.get("library.loans.created")
                .tag("outcome", "succeeded")
                .counter().count()).isEqualTo(1L);
    }

    @Test
    void lend_failedLoan_isCountedInMetric() {
        memberAndCopyExist();
        when(loanRepo.countByMemberIdAndReturnedAtIsNull(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.lend(1L,new LoanDtos.CreateLoanRequest( "BC-000001")))
                .isInstanceOf(LoanExceptions.LoanLimitExceededException.class);

        assertThat(registry.get("library.loans.created")
                .tag("outcome", "loan-limit-exceeded")
                .counter().count()).isEqualTo(1L);
    }

    @Test
    void returnLoan_returnedBook_isAvailable() {
        copy.markLoaned();

        when(loanRepo.findByIdAndMemberId(1L, 999L)).thenReturn(Optional.of(sampleLoan));

        service.returnLoan(1L, 999L, false);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void returnLoan_responseContainsDaysOverdue() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(5));

        when(loanRepo.findByIdAndMemberId(1L, 999L)).thenReturn(Optional.of(loan));

        LoanDtos.LoanResponse response = service.returnLoan(1L, 999L, false);

        assertThat(response.daysOverdue()).isEqualTo(5);
    }

    @Test
    void returnLoan_setsReturnedAtToady() {
        copy.markLoaned();
        Loan loan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(5));

        when(loanRepo.findByIdAndMemberId(1L, 999L)).thenReturn(Optional.of(loan));

        LoanDtos.LoanResponse response = service.returnLoan(1L, 999L, false);

        assertThat(response.returnedAt()).isEqualTo(TODAY);
    }

    @Test
    void returnLoan_alreadyReturnedLoan_throws() {
        copy.markLoaned();

        when(loanRepo.findByIdAndMemberId(1L, 999L)).thenReturn(Optional.of(sampleLoan));

        service.returnLoan(1L, 999L, false);

       assertThatThrownBy(() -> service.returnLoan(1L, 999L, false))
               .isInstanceOf(LoanExceptions.CopyAlreadyReturnedException.class);
    }

    @Test
    void returnLoan_lateReturn_IsCountedInMetric() {
        Loan lateLoan = new Loan(member, copy, TODAY.minusDays(10), TODAY.minusDays(1));
        copy.markLoaned();

        when(loanRepo.findByIdAndMemberId(1L, 9L))
                .thenReturn(Optional.of(lateLoan));

        service.returnLoan(1L, 9L, false);

        assertThat(registry.get("library.loans.returned")
                .tag("return_time", "late")
                .counter().count()).isEqualTo(1.0);
    }
}
