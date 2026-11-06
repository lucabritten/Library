package de.htwsaar.library.book;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.ISBN;
import org.springframework.data.domain.Page;

import java.util.List;

public class BookDtos {

    public record CreateBookRequest(

            @NotBlank(message = "Title cannot be blank.")
            String title,

            @NotBlank(message = "ISBN cannot be blank.")
            @ISBN(message = "ISBN is not valid.")
            String isbn,

            @NotBlank(message = "Author cannot be blank.")
            String author
    ) {}

    public record BookResponse(Long id, String title, String isbn, String author) {
        public static BookResponse from(Book book) {
            return new BookResponse(book.getId(),book.getTitle(), book.getIsbn(), book.getAuthor());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size,
                                  long totalElements, int totalPages) {
        static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                    p.getTotalElements(), p.getTotalPages());
        }
    }
}
