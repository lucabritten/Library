package de.htwsaar.library.book;

import de.htwsaar.library.author.Author;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.ISBN;


public class BookDtos {

    public record CreateBookRequest(

            @NotBlank(message = "Title cannot be blank.")
            @Size(max = 300)
            String title,

            @NotBlank(message = "ISBN cannot be blank.")
            @ISBN(message = "ISBN is not valid.")
            String isbn,

            @NotNull(message = "AuthorId cannot be blank.")
            @Positive
            Long authorId
    ) {}

    public record AuthorSummary(Long id, String name) {}

    public record BookResponse(Long id, String title, String isbn, AuthorSummary author) {
        public static BookResponse from(Book book) {
            Author author = book.getAuthor();
            return new BookResponse(book.getId(),book.getTitle(), book.getIsbn(),
                    new AuthorSummary(author.getId(), author.getFirstName() + " " + author.getLastName()));
        }
    }
}
