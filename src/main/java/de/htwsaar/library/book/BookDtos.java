package de.htwsaar.library.book;

import jakarta.validation.constraints.NotBlank;

public class BookDtos {

    public record CreateBookRequest(

            @NotBlank(message = "Title cannot be blank.")
            String title,

            @NotBlank(message = "ISBN cannot be blank.")
            String isbn,

            @NotBlank(message = "Author cannot be blank.")
            String author
    ) {}

    public record BookResponse(Long id, String title, String isbn, String author) {
        public static BookResponse from(Book book) {
            return new BookResponse(book.getId(),book.getTitle(), book.getIsbn(), book.getAuthor());
        }
    }
}
