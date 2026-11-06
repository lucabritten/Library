package de.htwsaar.library.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//not public, as it should only be accessed from inside the book package
@Repository
interface BookRepo extends JpaRepository<Book, Long> {

    Optional<Book> findByTitle(String title);

    Page<Book> findAllByAuthor(String author, Pageable pageable);

    Boolean existsByIsbn(String isbn);
}
