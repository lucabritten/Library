package de.htwsaar.library.loan;

import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.member.Member;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "LOAN")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq_gen")
    @SequenceGenerator(name = "loan_seq_gen", sequenceName = "loan_seq", allocationSize = 50)
    private Long id;

    @JoinColumn(name = "MEMBER_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @JoinColumn(name = "COPY_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BookCopy copy;

    @Column(nullable = false)
    private LocalDate loanedAt;

    @Column(nullable = false)
    private LocalDate dueAt;

    @Column(nullable = true)
    private LocalDate returnedAt;

    protected Loan() {
    }

    public Loan(Member member, BookCopy copy, LocalDate loanedAt, LocalDate dueAt) {
        if (dueAt.isBefore(loanedAt))
            throw new IllegalArgumentException("Book cannot be returned before being loaned.");
        this.member = member;
        this.copy = copy;
        this.loanedAt = loanedAt;
        this.dueAt = dueAt;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public BookCopy getCopy() {
        return copy;
    }

    public LocalDate getLoanedAt() {
        return loanedAt;
    }

    public LocalDate getDueAt() {
        return dueAt;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public void returnBook(LocalDate today) {
        if (returnedAt != null)
            throw new LoanExceptions.CopyAlreadyReturnedException(id);

        if(today.isBefore(loanedAt))
            throw new IllegalArgumentException("Book cannot be returned earlier than loaned.");

        returnedAt = today;
    }

    public long daysOverdue(LocalDate today) {
        LocalDate end = returnedAt != null ? returnedAt : today;
        return Math.max(0, ChronoUnit.DAYS.between(dueAt, end));
    }

    @Override
    public String toString() {
        return "Loan{id=" + id
                + ", memberId=" + (member != null ? member.getId() : null)
                + ", copyId=" + (copy != null ? copy.getId() : null)
                + ", loanedAt=" + loanedAt
                + ", dueAt=" + dueAt
                + ", returnedAt=" + returnedAt + "}";
    }
}
