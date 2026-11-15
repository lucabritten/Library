package de.htwsaar.library.copy;

import de.htwsaar.library.RepositoryTest;
import de.htwsaar.library.author.Author;
import de.htwsaar.library.book.Book;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookCopyRepoTest extends RepositoryTest {

    @Autowired
    BookCopyRepo repo;

    private BookCopy capitol;

    @BeforeEach
    void setup() {
        Author a1 = new Author("Karl", "Heinz");
        Author a2 = new Author("Bernd", "Stromberg");
        Book b1 = new Book("Capitol", "1234543212345", a2);
        Book b2 = new Book("Minecraft", "1234243212345", a2);
        capitol = new BookCopy(b1, "BC-000001");
        BookCopy capitol2 = new BookCopy(b1, "BC-000002");
        BookCopy minecraft = new BookCopy(b2, "BC-123456");

        given(a1,a2,b1,b2, capitol, capitol2, minecraft);
    }

    @Test
    void nextInventoryNumber_increments() {
        long lastNumber = repo.nextInventoryNumber();

        assertThat(repo.nextInventoryNumber()).isGreaterThan(lastNumber);
    }

    @Test
    void findByInventoryNumber_returnsCorrectCopy() {
        Optional<BookCopy> copyOptional = repo.findByInventoryNumber(capitol.getInventoryNumber());

        assertThat(copyOptional).isPresent();
        assertThat(copyOptional.get().getInventoryNumber()).isEqualTo(capitol.getInventoryNumber());
    }

    @Test
    void findByInventoryNumber_fetchesBookEagerly() {
        BookCopy copy = repo.findByInventoryNumber(capitol.getInventoryNumber()).get();

        assertThat(Hibernate.isInitialized(copy.getBook())).isTrue();
    }

    @Test
    void save_duplicateInventoryNumber_throwsDataIntegrityViolation() {
        BookCopy duplicate = new BookCopy(capitol.getBook(), capitol.getInventoryNumber());

        assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
