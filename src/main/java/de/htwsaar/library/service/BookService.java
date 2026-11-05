package de.htwsaar.library.service;

import de.htwsaar.library.model.Book;

import java.util.List;
import java.util.Optional;

public interface BookService {

    Optional<Book> findById(Long id);
    Book save(Book book);
    List<Book> findByAuthor(String author);


}
