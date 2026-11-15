package de.htwsaar.library.loan;

import de.htwsaar.library.author.Author;
import de.htwsaar.library.book.Book;
import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.member.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoanTest {

    private static final LocalDate LOANED = LocalDate.of(2026, 7 ,10);
    private static final LocalDate DUE = LocalDate.of(2026, 7, 24);

    private static Loan loanDueOn(LocalDate loanedAt, LocalDate dueAt) {
        Member member = new Member("Alice", "alice@hotmail.com");
        Author author = new Author("Michael", "Ende");
        Book book = new Book("Momo", "9783522202008", author);
        BookCopy copy = new BookCopy(book, "BC-00001");

        return new Loan(member,copy,loanedAt, dueAt);
    }

    @Test
    void loan_dueAtBeforeLoanedAt_throws() {
        assertThatThrownBy(() -> loanDueOn(DUE, LOANED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnBook_whenAlreadyReturned_throws() {
        Loan loan = loanDueOn(LOANED, DUE);
        LocalDate returnDate = DUE.minusDays(1);
        loan.returnBook(returnDate);

        assertThatThrownBy(() -> loan.returnBook(DUE.plusDays(1)))
                .isInstanceOf(LoanExceptions.CopyAlreadyReturnedException.class);

        assertThat(loan.getReturnedAt()).isEqualTo(returnDate);
    }

    @Test
    void returnBook_notReturnedYet_setsReturnedAt() {
        Loan loan = loanDueOn(LOANED, DUE);

        loan.returnBook(DUE);
        LocalDate returnedAt = loan.getReturnedAt();

        assertThat(returnedAt).isEqualTo(DUE);
    }

    @Test
    void returnBook_todayBeforeLoanedAt_throws() {
        Loan loan = loanDueOn(LOANED, DUE);

        assertThatThrownBy(() -> loan.returnBook(LOANED.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(loan.getReturnedAt()).isNull();
    }

    @Test
    void daysOverdue_beforeDueAt_isZero() {
        Loan loan = loanDueOn(LOANED, DUE);

        LocalDate before = DUE.minusDays(1);

        long daysOverdue = loan.daysOverdue(before);

        assertThat(daysOverdue).isZero();
    }

    @Test
    void daysOverdue_notReturnedAndOverdue_countsDaysUntilToday() {
        Loan loan = loanDueOn(LOANED, DUE);

        LocalDate plusOne = DUE.plusDays(1);
        LocalDate plusTwo = DUE.plusDays(2);

        assertThat(loan.daysOverdue(plusOne)).isEqualTo(1L);
        assertThat(loan.daysOverdue(plusTwo)).isEqualTo(2L);
    }

    @Test
    void daysOverdue_returned_isFixedToReturnDate() {
        Loan loan = loanDueOn(LOANED, DUE);

        LocalDate returnDate = DUE.plusDays(1);
        LocalDate returnDatePlusOne = returnDate.plusDays(1);
        loan.returnBook(returnDate);

        assertThat(loan.daysOverdue(returnDate)).isEqualTo(1L);
        assertThat(loan.daysOverdue(returnDatePlusOne)).isEqualTo(1L);
    }

    @Test
    void daysOverdue_earlyReturnedBook_staysZero() {
        Loan loan = loanDueOn(LOANED, DUE);
        LocalDate returnDate = DUE.minusDays(1);
        LocalDate laterLookup = DUE.plusDays(10);

        loan.returnBook(returnDate);

        assertThat(loan.daysOverdue(laterLookup)).isZero();
    }

    @Test
    void daysOverdue_openOnDueAt_isZero() {
        assertThat(loanDueOn(LOANED, DUE).daysOverdue(DUE)).isZero();
    }

}
