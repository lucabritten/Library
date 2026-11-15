package de.htwsaar.library.author;

import de.htwsaar.library.RepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorRepoTest extends RepositoryTest {

    @Autowired AuthorRepo authorRepo;

    @BeforeEach
    void setUp() {
        given(
                new Author("J.R.R.", "Tolkien"),
                new Author("Christopher", "Tolkien"),
                new Author("Michael", "Ende"),
                new Author("Franz", "Kafka")
        );
    }

    @ParameterizedTest(name = "\"{0}\" findet beide Tolkiens")
    @ValueSource(strings = {"Tolkien", "tolkien", "TOLK", "olki"})
    void findByLastName_partialAndCaseInsensitive_findsMatches(String search) {
        Page<Author> result = authorRepo.findAllByLastNameContainingIgnoreCase(
                search, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Author::getLastName)
                .containsOnly("Tolkien");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByLastName_noMatch_returnsEmptyPage() {
        Page<Author> result = authorRepo.findAllByLastNameContainingIgnoreCase(
                "Rowling", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findByLastName_withPaging_returnsRequestedPageAndTotal() {
        given(new Author("Max", "Ender"));

        Page<Author> firstPage = authorRepo.findAllByLastNameContainingIgnoreCase(
                "end", PageRequest.of(0, 1, Sort.by("lastName")));

        assertThat(firstPage.getContent())
                .extracting(Author::getLastName)
                .containsExactly("Ende");
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void findByLastName_sortsByFirstNameDescending() {
        Page<Author> result = authorRepo.findAllByLastNameContainingIgnoreCase(
                "tolkien", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "firstName")));

        assertThat(result.getContent())
                .extracting(Author::getFirstName)
                .containsExactly("J.R.R.", "Christopher");
    }
}