package de.htwsaar.library.book;

import de.htwsaar.library.book.BookDtos.BookResponse;
import de.htwsaar.library.book.BookDtos.CreateBookRequest;

import de.htwsaar.library.book.BookExceptions.BookNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

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
        //Further validation e.g. book with isbn already exists --> custom exception, repo-methods like existsByIsbn
        String isbn = bookProperties.normalizeIsbn() ? req.isbn().replace("-", "") : req.isbn();
        Book saved = bookRepo.save(new Book(req.title(), isbn, req.author()));

        logger.info("Created book {}", saved);
        return BookResponse.from(saved);

    }
}
