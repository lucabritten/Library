package de.htwsaar.library.loan;


import de.htwsaar.library.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class LoanFlowIT extends IntegrationTest {



    @Test
    void lendAndReturn() {
        long book = createBook(createAuthor());
        String copy = createCopy(book);
        TestMember alice = registerAndLogin();

        // Loan book
        LoanDtos.LoanResponse loan = client.post().uri(BASE_URI + "/loans")
                .headers(h -> h.setBearerAuth(alice.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(copy))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoanDtos.LoanResponse.class).returnResult().getResponseBody();

        //return book
        client.post().uri(BASE_URI + "/loans/{id}/return", loan.id())
                .headers(h -> h.setBearerAuth(alice.token()))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.returnedAt").isNotEmpty();

        // check if book is available again
        client.get().uri(BASE_URI + "/copies/{nr}", copy)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.copyStatus").isEqualTo("AVAILABLE");
    }

    @Test
    void lend_onlyAccessableFromUser_otherwise404() {
        TestMember alice = registerAndLogin();
        TestMember bob = registerAndLogin();

        long book = createBook(createAuthor());
        String copy = createCopy(book);

        // Alice lends a copy
        long loanId = client.post().uri(BASE_URI + "/loans")
                .headers(h -> h.setBearerAuth(alice.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(copy))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoanDtos.LoanResponse.class)
                .returnResult().getResponseBody()
                .id();

        // Bob tries to access
        client.get().uri(BASE_URI + "/loans/{id}", loanId)
                .headers(h -> h.setBearerAuth(bob.token()))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}
