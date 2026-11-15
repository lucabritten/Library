package de.htwsaar.library.author;

import de.htwsaar.library.IntegrationTest;
import org.junit.jupiter.api.Test;

class AuthorDeleteIT extends IntegrationTest {

    @Test
    void deleteAuthorWithBooks_returns409_andAuthorStillExists() {
        long author = createAuthor();
        createBook(author);

        client.delete().uri(BASE_URI + "/authors/{id}", author)
                .exchange()
                .expectStatus().isEqualTo(409);

        client.get().uri(BASE_URI + "/authors/{id}", author)
                .exchange()
                .expectStatus().isOk();      // Rollback hat gegriffen
    }

    @Test
    void deleteAuthorWithoutBooks_returns204_andIsGone() {
        long author = createAuthor();

        client.delete().uri(BASE_URI + "/authors/{id}", author)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri(BASE_URI + "/authors/{id}", author)
                .exchange()
                .expectStatus().isNotFound();
    }
}