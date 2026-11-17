package de.htwsaar.library.auth;

import de.htwsaar.library.IntegrationTest;
import de.htwsaar.library.loan.LoanDtos;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class AuthIT extends IntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(AuthIT.class);


    @Test
    void login_validCredentials_returnsBearerToken() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        login(email, PASSWORD)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jwtToken").isNotEmpty()
                .jsonPath("$.authType").isEqualTo("Bearer");
    }

    @Test
    void login_wrongPassword_returns401() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        login(email, "WRONG_PASSWORD").expectStatus().isUnauthorized();
    }

    @Test
    void login_wrongEmail_returns401() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        login("another@email.com", PASSWORD).expectStatus().isUnauthorized();
    }

    @Test
    void admin_hasPrivilegeToReturnBook() {
        String userToken = registerAndLogin().token();
        String adminToken = adminToken();

        long book = createBook(createAuthor());
        String copy = createCopy(book);

        long loanId = client.post().uri(BASE_URI + "/loans")
                .headers(h -> h.setBearerAuth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(copy))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoanDtos.LoanResponse.class)
                .returnResult().getResponseBody()
                .id();

        client.post().uri(BASE_URI + "/loans/{id}/return", loanId)
                .headers(h -> h.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk();
    }
}
