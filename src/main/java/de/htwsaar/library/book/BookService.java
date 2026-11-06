package de.htwsaar.library.book;

import de.htwsaar.library.book.BookDtos.BookResponse;
import de.htwsaar.library.book.BookDtos.CreateBookRequest;
import de.htwsaar.library.book.BookDtos.PageResponse;


import de.htwsaar.library.book.BookExceptions.BookNotFoundException;
import de.htwsaar.library.book.BookExceptions.DuplicateIsbnException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true) //Standard für die Klasse, abweichende (schreibende) Methoden brauchen nochmal Annotation
public class BookService {

    private final Logger logger =
            LoggerFactory.getLogger(BookService.class);

    private final BookRepo bookRepo;
    private final BookProperties bookProperties;

    public BookService(BookRepo bookRepo, BookProperties bookProperties) {
        this.bookRepo = bookRepo;
        this.bookProperties = bookProperties;
    }

    public PageResponse<BookResponse> search(String author, Pageable pageable) {
        Page<Book> books;
        if (StringUtils.hasText(author))
            books = bookRepo.findAllByAuthor(author, pageable);

        else
            books = bookRepo.findAll(pageable);

        return PageResponse.from(books.map(BookResponse::from));
    }

    public BookResponse findById(Long id) {
        return bookRepo.findById(id)
                .map(BookDtos.BookResponse::from)
                .orElseThrow(() -> {
                    logger.info("Book with id={} not found", id);
                    return new BookNotFoundException(id);
                });
    }

    @Transactional
    public BookResponse create(CreateBookRequest req) {
        String isbn = normalize(req.isbn());

        if (bookRepo.existsByIsbn(isbn))
            throw new DuplicateIsbnException(req.isbn());

        try {
            Book saved = bookRepo.saveAndFlush(new Book(req.title(), isbn, req.author()));
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
}
