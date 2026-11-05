package de.htwsaar.library.service;


import de.htwsaar.library.model.Book;
import de.htwsaar.library.repo.BookRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService{

    private final static Logger log =
            LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookRepo bookRepo;

    public BookServiceImpl(@Autowired BookRepo bookRepo) { //Autowired optional bei einem Konstruktor
        this.bookRepo = bookRepo;
    }

    @Override
    public Optional<Book> findById(Long id) {
        log.debug("Suche Buch mit ID={}", id);
        return bookRepo.findById(id);
    }

    @Override
    public Book save(Book book) {
        return bookRepo.save(book);
    }

    @Override
    public List<Book> findByAuthor(String author) {
        return bookRepo.findByAuthor(author);
    }
}
