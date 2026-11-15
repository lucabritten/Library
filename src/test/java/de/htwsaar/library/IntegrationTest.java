package de.htwsaar.library;

import de.htwsaar.library.author.AuthorDtos;
import de.htwsaar.library.book.BookDtos;
import de.htwsaar.library.copy.BookCopyDto;
import de.htwsaar.library.member.MemberDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.concurrent.atomic.AtomicInteger;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainerConfig.class)
public abstract class IntegrationTest {

    @Autowired
    protected RestTestClient client;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    protected static final String BASE_URI = "/api/v1";

    protected long createAuthor() {
        return client.post().uri("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthorDtos.CreateAuthorRequest("Michael", "Ende"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthorDtos.AuthorResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected long createBook(long authorId) {
        return client.post().uri("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookDtos.CreateBookRequest("Momo", uniqueIsbn(), authorId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookDtos.BookResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected String createCopy(long bookId) {
        return client.post().uri("/api/v1/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookCopyDto.CreateBookCopyRequest(bookId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookCopyDto.BookCopyResponse.class)
                .returnResult().getResponseBody().inventoryNumber();
    }

    protected long createMember() {
        String email = "member" + COUNTER.incrementAndGet() + "@example.com";
        return client.post().uri("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MemberDtos.CreateMemberRequest("Test", email))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MemberDtos.MemberResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected static String uniqueIsbn() {
        String base = "978" + "%09d".formatted(COUNTER.incrementAndGet());
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = base.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return base + (10 - sum % 10) % 10;
    }
}
