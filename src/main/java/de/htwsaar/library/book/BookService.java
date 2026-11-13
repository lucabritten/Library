package de.htwsaar.library.book;

import de.htwsaar.library.author.Author;
import de.htwsaar.library.author.AuthorService;
import de.htwsaar.library.book.BookDtos.BookResponse;
import de.htwsaar.library.book.BookDtos.CreateBookRequest;
import de.htwsaar.library.common.Dtos.PageResponse;


import de.htwsaar.library.book.BookExceptions.BookNotFoundException;
import de.htwsaar.library.book.BookExceptions.DuplicateIsbnException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true) //Standard für die Klasse, abweichende (schreibende) Methoden brauchen nochmal Annotation
public class BookService {

    private final static Logger logger =
            LoggerFactory.getLogger(BookService.class);

    private final AuthorService authorService;

    private final BookRepo bookRepo;
    private final BookProperties bookProperties;

    public BookService(AuthorService authorService, BookRepo bookRepo, BookProperties bookProperties) {
        this.authorService = authorService;
        this.bookRepo = bookRepo;
        this.bookProperties = bookProperties;
    }

    public PageResponse<BookResponse> search(Long authorId, Pageable pageable) {
        Page<Book> bookPage;
        if (authorId != null)
            bookPage = bookRepo.findAllByAuthorId(authorId, pageable);

        else
            bookPage = bookRepo.findAll(pageable);

        return PageResponse.from(bookPage.map(BookResponse::from));
    }

    public BookResponse findById(Long id) {
        return BookResponse.from(require(id));
    }

    @Transactional
    public BookResponse create(CreateBookRequest req) {
        String isbn = normalize(req.isbn());

        if (bookRepo.existsByIsbn(isbn))
            throw new DuplicateIsbnException(isbn);

        Author author = authorService.require(req.authorId());
        try {
            Book saved = bookRepo.saveAndFlush(new Book(req.title(), isbn, author));
            logger.info("Created book id={}, isbn={}", saved.getId(), saved.getIsbn());
            return BookResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIsbnException(isbn);
        }
    }

    private String normalize(String isbn) {
        return bookProperties.normalizeIsbn() ? isbn.replace("-", "") : isbn;
    }

    @Transactional
    public void delete(Long id) {
        bookRepo.deleteById(id);
    }

    //SChnittstelle zu BookCopy
    public Book require(Long id) {
        return bookRepo.findById(id)
                .orElseThrow(() -> {
                    logger.info("Book with id={} not found.", id);
                    return new BookNotFoundException(id);
                });
    }
}
