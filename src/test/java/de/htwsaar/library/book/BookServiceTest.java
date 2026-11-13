package de.htwsaar.library.book;

import de.htwsaar.library.author.Author;
import de.htwsaar.library.author.AuthorExceptions;
import de.htwsaar.library.author.AuthorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    BookRepo bookRepo;

    @Mock
    AuthorService authorService;

    private final static Pageable page = PageRequest.of(0, 10);

    private BookService serviceWith(boolean normalizeISBN) {
        return new BookService(authorService, bookRepo, new BookProperties(normalizeISBN));
    }

    @Test
    void search_withoutAuthorId_usesFindAll() {
        BookService bookService = serviceWith(false);
        when(bookRepo.findAll(page))
                .thenReturn(Page.empty());

        bookService.search(null, page);

        verify(bookRepo).findAll(page);
        verify(bookRepo, never()).findAllByAuthorId(any(), any());
    }

    @Test
    void search_withAuthorId_usesFilteredQuery() {
        BookService bookService = serviceWith(false);
        Long id = 123L;
        when(bookRepo.findAllByAuthorId(id, page))
                .thenReturn(Page.empty());

        bookService.search(id, page);

        verify(bookRepo).findAllByAuthorId(id, page);
        verify(bookRepo, never()).findAll(page);
    }

    @ParameterizedTest(name = "normalize={0}: \"{1}\" → \"{2}\"")
    @CsvSource({
            "true,  978-3-522-20200-8, 9783522202008",
            "true,  9783522202008,     9783522202008",   // schon normalisiert, bleibt gleich
            "false, 978-3-522-20200-8, 978-3-522-20200-8"
    })
    void create_withHyphens_savesNormalizedIsbn(boolean normalized, String input, String expected) {
        BookService bookService = serviceWith(normalized);
        Long authorId = 123L;
        when(authorService.require(authorId))
                .thenReturn(new Author("Tim", "Cook"));
        when(bookRepo.saveAndFlush(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.create(new BookDtos.CreateBookRequest("Success of Apple", input, authorId));

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getIsbn()).isEqualTo(expected);
    }

    @Test
    void create_isbnAlreadyExists_throwsDuplicateIsbn() {
        BookService bookService = serviceWith(false);
        String isbn = "9783522202008";
        when(bookRepo.existsByIsbn(isbn))
                .thenThrow(BookExceptions.DuplicateIsbnException.class);



        assertThatThrownBy(() -> bookService.create(new BookDtos.CreateBookRequest("Success of Apple", isbn, 123L)))
                .isInstanceOf(BookExceptions.DuplicateIsbnException.class);
        verify(bookRepo, never()).saveAndFlush(any());
    }

    @Test
    void create_isbnWithHyphensExistsNormalized_throwsDuplicate() {
        BookService service = serviceWith(true);
        when(bookRepo.existsByIsbn("9783522202008")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new BookDtos.CreateBookRequest("Momo", "978-3-522-20200-8", 1L)))
                .isInstanceOf(BookExceptions.DuplicateIsbnException.class);

        verify(bookRepo, never()).saveAndFlush(any());
    }

    @Test
    void create_unknownAuthor_throws() {
        BookService bookService = serviceWith(false);
        long authorID = 123L;
        when(authorService.require(authorID))
                .thenThrow(AuthorExceptions.AuthorNotFoundException.class);

        assertThatThrownBy(() -> bookService.create(new BookDtos.CreateBookRequest("Success of Apple", "1234", authorID)))
                .isInstanceOf(AuthorExceptions.AuthorNotFoundException.class);
        verify(bookRepo, never()).saveAndFlush(any());
    }

    @Test
    void create_duplicateIsbnWhileSaving_throws() {
        BookService bookService = serviceWith(false);
        when(authorService.require(123L))
                .thenReturn(new Author("Tim", "Cook"));
        when(bookRepo.saveAndFlush(any()))
                .thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> bookService.create(new BookDtos.CreateBookRequest("Success of Apple", "1234", 123L)))
                .isInstanceOf(BookExceptions.DuplicateIsbnException.class);
    }

    @Test
    void require_noAuthorWithId_throwsAuthorNotFound() {
        long id = 123L;
        BookService bookService = serviceWith(false);
        when(bookRepo.findById(id))
                .thenThrow(BookExceptions.BookNotFoundException.class);

        assertThatThrownBy(() -> bookService.require(id))
                .isInstanceOf(BookExceptions.BookNotFoundException.class);
    }

    @Test
    void findById_noAuthorWithId_throwsAuthorNotFound() {
        long id = 123L;
        BookService bookService = serviceWith(false);
        when(bookRepo.findById(id))
                .thenThrow(BookExceptions.BookNotFoundException.class);

        assertThatThrownBy(() -> bookService.findById(id))
                .isInstanceOf(BookExceptions.BookNotFoundException.class);
    }
}
