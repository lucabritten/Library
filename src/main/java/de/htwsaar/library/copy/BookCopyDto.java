package de.htwsaar.library.copy;

import de.htwsaar.library.book.Book;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BookCopyDto {

    public record CreateBookCopyRequest (
            @NotNull
            @Positive
            Long bookId
    ) {}

    public record BookSummary(String title, String isbn) {}

    public record BookCopyResponse(
            Long id,
            BookSummary bookSummary,
            String inventoryNumber,
            CopyStatus copyStatus
    ) {
        public static BookCopyResponse from(BookCopy copy) {
            Book book = copy.getBook();
            return new BookCopyResponse(
                    copy.getId(),
                    new BookSummary(book.getTitle(), book.getIsbn()),
                    copy.getInventoryNumber(),
                    copy.getStatus()
            );
        }
    }
}
