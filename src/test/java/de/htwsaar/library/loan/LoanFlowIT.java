package de.htwsaar.library.loan;


import de.htwsaar.library.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public class LoanFlowIT extends IntegrationTest {



    @Test
    void lendAndReturn() {
        long book = createBook(createAuthor());
        String copy = createCopy(book);
        long member = createMember();

        // Loan book
        LoanDtos.LoanResponse loan = client.post().uri(BASE_URI + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(member, copy))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoanDtos.LoanResponse.class).returnResult().getResponseBody();

        //return book
        client.post().uri(BASE_URI + "/loans/{id}/return", loan.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.returnedAt").isNotEmpty();

        // check if book is available again
        client.get().uri(BASE_URI + "/copies/{nr}", copy)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.copyStatus").isEqualTo("AVAILABLE");
    }
}
