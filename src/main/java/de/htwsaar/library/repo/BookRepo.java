package de.htwsaar.library.repo;

import de.htwsaar.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<Book, Long> {

    Book findByName(String name);

    List<Book> findByAuthor(String author);
}
