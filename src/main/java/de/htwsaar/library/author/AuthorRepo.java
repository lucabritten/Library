package de.htwsaar.library.author;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthorRepo extends JpaRepository<Author, Long> {

    Page<Author> findAllByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
}
