package de.htwsaar.library.copy;

import de.htwsaar.library.book.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface BookCopyRepo extends JpaRepository<BookCopy, Long> {
    Long countByBook(Book book);

    @Query(value = "SELECT nextval('inventory_number_seq')", nativeQuery = true)
    long nextInventoryNumber();

    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findByInventoryNumber(String inventoryNumber);
    void deleteBookCopyByInventoryNumber(String inventoryNumber);
}
