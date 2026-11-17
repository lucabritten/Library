package de.htwsaar.library.loan;

import de.htwsaar.library.RepositoryTest;
import de.htwsaar.library.author.Author;
import de.htwsaar.library.book.Book;
import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.member.Member;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class LoanRepoTest extends RepositoryTest {

    @Autowired
    LoanRepo repo;

    Member max;
    Member erika;

    BookCopy c1;
    BookCopy c3;

    private static final LocalDate LOANED = LocalDate.of(2026,9,9);
    private static final LocalDate DUE = LocalDate.of(2026,10,9);

    Loan openLoan;

    @BeforeEach
    void setUp() {
        max   = new Member("Max", "max@example.com", "password_hash");
        erika = new Member("Erika", "erika@example.com", "password_hash");
        Author author = new Author("Walter", "White");
        Book book = new Book("Breaking Bad", "9781617290459", author);
        c1 = new BookCopy(book, "BC-000001");
        BookCopy c2 = new BookCopy(book, "BC-000002");
        c3 = new BookCopy(book, "BC-000003");
        BookCopy c4 = new BookCopy(book, "BC-000004");

        openLoan = new Loan(max, c1, LOANED, DUE);
        Loan maxSecondOpen = new Loan(max, c2, LOANED, DUE);
        Loan maxReturned = new Loan(max, c3, LOANED, DUE);
        maxReturned.returnBook(DUE);
        Loan erikaOpen = new Loan(erika, c4, LOANED, DUE);

        given(max, erika, author, book, c1, c2, c3, c4,
                openLoan, maxSecondOpen, maxReturned, erikaOpen);
    }

    @Test
    void countOpen_countsOnlyOpenLoansOfThatMember() {
        // Max: 2 offen + 1 zurückgegeben, Erika: 1 offen
        assertThat(repo.countByMemberIdAndReturnedAtIsNull(max.getId())).isEqualTo(2);
    }

    @Test
    void countOpen_otherMember_isCountedSeparately() {
        assertThat(repo.countByMemberIdAndReturnedAtIsNull(erika.getId())).isEqualTo(1);
    }

    @Test
    void findById_copyIsInitialized() {
        Optional<Loan> loan = repo.findById(openLoan.getId());

        assertThat(loan).isPresent();
        assertThat(Hibernate.isInitialized(loan.get().getCopy())).isTrue();
    }

    @Test
    void save_secondOpenLoanForSameCopy_throwsDataIntegrityViolation() {
        Loan duplicate = new Loan(erika, c1, LOANED, DUE);    // c1 ist noch an Max verliehen

        assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_newLoanForReturnedCopy_isAllowed() {
        Loan again = new Loan(erika, c3, DUE.plusDays(1), DUE.plusDays(15));   // c3 wurde zurückgegeben

        Loan saved = repo.saveAndFlush(again);

        assertThat(repo.findById(saved.getId())).isPresent();
    }
}
