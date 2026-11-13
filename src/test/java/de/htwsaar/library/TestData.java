package de.htwsaar.library;

import de.htwsaar.library.author.Author;
import de.htwsaar.library.book.Book;
import de.htwsaar.library.copy.BookCopy;
import de.htwsaar.library.copy.CopyStatus;
import de.htwsaar.library.member.Member;

public class TestData {
    public static Book aBook() {
        return new Book("Momo", "9783522202008", new Author("Michael", "Ende"));
    }

    public static BookCopy aCopy() {
        return new BookCopy(aBook(), "BC-000001");
    }

    public static BookCopy copyIn(CopyStatus status) {
        BookCopy copy = aCopy();
        switch (status) {
            case AVAILABLE -> { }
            case LOANED    -> copy.markLoaned();
            case DAMAGED   -> copy.markDamaged();
            case WITHDRAWN -> copy.markWithdrawn();
        }
        return copy;
    }

    public static Member aMember() {
        return new Member("Testuser", "test@example.com");
    }


}
