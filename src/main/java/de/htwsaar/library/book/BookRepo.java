package de.htwsaar.library.book;

import de.htwsaar.library.author.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//not public, as it should only be accessed from inside the book package
@Repository
interface BookRepo extends JpaRepository<Book, Long> {

    Optional<Book> findByTitle(String title);

    @EntityGraph(attributePaths = "author") // Lazy Loading --> Author wird bei den Anfragen immer gebraucht, daher direkt mitfetchen
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findAllByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findAllByAuthorLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    boolean existsByIsbn(String isbn);
}
