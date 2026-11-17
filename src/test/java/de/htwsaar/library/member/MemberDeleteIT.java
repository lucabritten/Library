package de.htwsaar.library.member;

import de.htwsaar.library.IntegrationTest;
import de.htwsaar.library.loan.LoanDtos;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class MemberDeleteIT extends IntegrationTest {

    @Test
    void deleteMe_thenOldTokenFindsNothing() {
        TestMember m = registerAndLogin();

        client.delete().uri(BASE_URI + "/members/me")
                .headers(h -> h.setBearerAuth(m.token()))
                .exchange().expectStatus().isNoContent();

        client.get().uri(BASE_URI + "/members/me")
                .headers(h -> h.setBearerAuth(m.token()))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void memberWithLoans_leadsTo409() {
        long book = createBook(createAuthor());
        String copy = createCopy(book);
        TestMember member = registerAndLogin();

        client.post().uri(BASE_URI + "/loans");

        LoanDtos.LoanResponse loan = client.post().uri(BASE_URI + "/loans")
                .headers(h -> h.setBearerAuth(member.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(copy))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoanDtos.LoanResponse.class).returnResult().getResponseBody();

        client.post().uri(BASE_URI + "/loans/{id}/return", loan.id())
                .headers(h -> h.setBearerAuth(member.token()))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.returnedAt").isNotEmpty();

        client.delete().uri(BASE_URI + "/members/me")
                .headers(h -> h.setBearerAuth(member.token()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
