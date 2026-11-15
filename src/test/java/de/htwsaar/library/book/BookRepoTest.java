package de.htwsaar.library.book;

import de.htwsaar.library.RepositoryTest;
import de.htwsaar.library.author.Author;
import de.htwsaar.library.copy.BookCopy;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookRepoTest extends RepositoryTest {

    @Autowired
    BookRepo repo;

    private Book starWars;

    @BeforeEach
    void setUp() {
        Author einstein = new Author("Albert", "Einstein");
        Author mustermann = new Author("Max", "Mustermann");
        starWars = new Book("Star Wars", "9781617290459", einstein);

        given(einstein, mustermann, starWars,
                new Book("Minecraft", "9781617294945", mustermann));
    }

    @Test
    void findById_fetchesAuthorEagerly() {
        Book book = repo.findById(starWars.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(book.getAuthor())).isTrue();
        assertThat(book.getAuthor().getLastName()).isEqualTo("Einstein");
    }

    @Test
    void save_duplicateIsbn_throwsDataIntegrityViolation() {
        Book duplicate = new Book("Anderer Titel", starWars.getIsbn(), starWars.getAuthor());

        assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_bookWithCopies_triggersConstraint() {
        given(new BookCopy(starWars, "BC-000123"));

        assertThatThrownBy(() -> {
            repo.deleteById(starWars.getId());
            repo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_bookWithoutCopies_isRemoved() {
        repo.deleteById(starWars.getId());
        repo.flush();

        assertThat(repo.findById(starWars.getId())).isEmpty();
    }

    @Test
    void findAll_fetchesAuthorsForWholePage() {
        Page<Book> page = repo.findAll(PageRequest.of(0, 10, Sort.by("title")));

        assertThat(page.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Minecraft", "Star Wars");
        assertThat(page.getContent())
                .allSatisfy(book -> assertThat(Hibernate.isInitialized(book.getAuthor())).isTrue());
    }

    @Test
    void findAll_withPaging_returnsTotalAcrossPages() {
        Page<Book> page = repo.findAll(PageRequest.of(0, 1, Sort.by("title")));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAllByAuthorId_returnsOnlyBooksOfThatAuthor() {
        Page<Book> page = repo.findAllByAuthorId(
                starWars.getAuthor().getId(), PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Book::getTitle)
                .containsOnly("Star Wars");
        assertThat(page.getContent())
                .allSatisfy(book -> assertThat(Hibernate.isInitialized(book.getAuthor())).isTrue());
    }

    @Test
    void findAll_unknownSortProperty_throwsPropertyReference() {
        assertThatThrownBy(() -> repo.findAll(PageRequest.of(0, 10, Sort.by("foo"))))
                .isInstanceOf(PropertyReferenceException.class);
    }
}
