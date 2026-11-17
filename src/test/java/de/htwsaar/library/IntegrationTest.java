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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainerConfig.class)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected RestTestClient client;

    @Autowired
    private JwtEncoder jwtEncoder;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    protected static final String BASE_URI = "/api/v1";

    protected static final String PASSWORD = "correct-horse-battery";

    protected long createAuthor() {
        return client.post().uri("/api/v1/authors")
                .headers(h -> h.setBearerAuth(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthorDtos.CreateAuthorRequest("Michael", "Ende"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthorDtos.AuthorResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected long createBook(long authorId) {
        return client.post().uri("/api/v1/books")
                .headers(h -> h.setBearerAuth(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookDtos.CreateBookRequest("Momo", uniqueIsbn(), authorId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookDtos.BookResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected String createCopy(long bookId) {
        return client.post().uri("/api/v1/copies")
                .headers(h -> h.setBearerAuth(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookCopyDto.CreateBookCopyRequest(bookId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookCopyDto.BookCopyResponse.class)
                .returnResult().getResponseBody().inventoryNumber();
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

    protected String uniqueEmail() {
        return "member" + COUNTER.incrementAndGet() + "@lib.com";
    }

    protected record TestMember(long id, String token) { }

    protected long register(String email, String password) {
        return client.post().uri(BASE_URI + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MemberDtos.CreateMemberRequest("Test", email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MemberDtos.MemberResponse.class)
                .returnResult().getResponseBody().id();
    }

    protected  RestTestClient.ResponseSpec login(String email, String password) {
        return client.post().uri(BASE_URI + "/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .exchange();

    }

    protected TestMember registerAndLogin() {
        String email = uniqueEmail();
        long id = register(email, PASSWORD);

        String token = (String) login(email, PASSWORD)
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult().getResponseBody()
                .get("jwtToken");

        return new TestMember(id, token);
    }

    protected String adminToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("library-api").subject("0").audience(List.of("library"))
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("scope", "catalog:write admin loans:write")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
