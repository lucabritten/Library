package de.htwsaar.library.copy;

import de.htwsaar.library.book.Book;
import de.htwsaar.library.copy.BookCopyExceptions.*;

import jakarta.persistence.*;

@Entity
@Table(name = "BOOK_COPY")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_copy_seq_gen")
    @SequenceGenerator(name = "book_copy_seq_gen", sequenceName = "book_copy_seq", allocationSize = 50)
    private Long id;

    @JoinColumn(name =  "BOOK_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Book book;

    @Column(nullable = false, unique = true)
    private String inventoryNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private CopyStatus copyStatus = CopyStatus.AVAILABLE;

    @Version
    private long version;

    protected BookCopy() { }

    public BookCopy(Book book, String inventoryNumber) {
        this.book = book;
        this.inventoryNumber = inventoryNumber;
    }

    public Book getBook() {
        return book;
    }

    public Long getId() {
        return id;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public CopyStatus getStatus() {
        return copyStatus;
    }

    public void markLoaned() {
        if (copyStatus != CopyStatus.AVAILABLE ) throw new CopyNotAvailableException(id, inventoryNumber);

        copyStatus = CopyStatus.LOANED;
    }

    public void markReturned() {
        if (copyStatus != CopyStatus.LOANED)
            throw new CopyNotAvailableException("Copy not marked as loaned. Please double check.");

        copyStatus = CopyStatus.AVAILABLE;
    }

    public void markWithdrawn() {
        if (copyStatus != CopyStatus.AVAILABLE && copyStatus != CopyStatus.DAMAGED)
            throw new BookCopyExceptions.CopyNotAvailableException("Unavailable or not damaged book cannot be withdrawn");

        copyStatus = CopyStatus.WITHDRAWN;
    }

    public void markDamaged() {
        if (copyStatus != CopyStatus.AVAILABLE)
            throw new CopyNotAvailableException("Please return book before marking as DAMAGED");

        copyStatus = CopyStatus.DAMAGED;
    }

    public void markRepaired() {
        if (copyStatus != CopyStatus.DAMAGED)
            throw new CopyNotAvailableException("A non-damaged book cannot be repaired.");

        copyStatus = CopyStatus.AVAILABLE;
    }
}
