package de.htwsaar.library.loan;

import de.htwsaar.library.IntegrationTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentLoanIT extends IntegrationTest {

    @RepeatedTest(5)
    void concurrentLend_sameCopy_exactlyOneSucceeds() throws Exception {

        String copy = createCopy(createBook(createAuthor()));
        long alice = createMember();
        long bob = createMember();

        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)){
            Future<Integer> a = pool.submit(() -> { start.await(); return lend(copy, alice); });
            Future<Integer> b = pool.submit(() -> { start.await(); return lend(copy, bob); });
            start.countDown();

            assertThat(assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(201, 409));
        }
    }

    Integer lend(String copy, Long member) {
        return client.post().uri(BASE_URI + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoanDtos.CreateLoanRequest(member, copy))
                .exchange()
                .returnResult()
                .getStatus().value();
    }

}
