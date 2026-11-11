package de.htwsaar.library.copy;

import de.htwsaar.library.book.Book;
import de.htwsaar.library.book.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookCopyService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookCopyService.class);

    private final BookCopyRepo bookCopyRepo;

    private final BookService bookService;

    public BookCopyService(BookCopyRepo bookCopyRepo, BookService bookService) {
        this.bookCopyRepo = bookCopyRepo;
        this.bookService = bookService;
    }

    public BookCopyDto.BookCopyResponse findByInventoryNumber(String inventoryNumber) {
        return BookCopyDto.BookCopyResponse.from(require(inventoryNumber));
    }

    public BookCopy require(String inventoryNumber) {
        return bookCopyRepo.findByInventoryNumber(inventoryNumber)
                .orElseThrow(() -> {
                    logger.info("Book-copy with inventory-number={} not found.", inventoryNumber);
                    return new BookCopyExceptions.BookCopyNotFoundException(inventoryNumber);
                });
    }

    @Transactional
    public BookCopyDto.BookCopyResponse create(BookCopyDto.CreateBookCopyRequest req) {
        Book book = bookService.require(req.bookId());

        String inventoryNumber = "BC-%06d".formatted(bookCopyRepo.nextInventoryNumber());

        BookCopy saved = bookCopyRepo.saveAndFlush(new BookCopy(book, inventoryNumber));
        return BookCopyDto.BookCopyResponse.from(saved);
    }

    @Transactional
    public void withdrawCopy(String inventoryNumber) {
        BookCopy copy = bookCopyRepo.findByInventoryNumber(inventoryNumber)
                        .orElseThrow(() ->  new BookCopyExceptions.BookCopyNotFoundException(inventoryNumber));

        if (copy.getStatus() != CopyStatus.AVAILABLE)
            throw new BookCopyExceptions.CopyNotAvailableException("Copy with inventory-number=" + inventoryNumber + " cannot be deleted as it is currently loaned");

        copy.markWithdrawn();
    }
}
