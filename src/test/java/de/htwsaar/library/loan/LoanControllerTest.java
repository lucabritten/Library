package de.htwsaar.library.loan;

import de.htwsaar.library.common.Exceptions;
import de.htwsaar.library.copy.BookCopyExceptions;
import de.htwsaar.library.member.MemberExceptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(LoanController.class)
public class LoanControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    LoanService loanService;

    private static final String BASE_URI = "/api/v1/loans";

    @Test
    void getLoan_returnsLoanASJson() {
        when(loanService.findById(any()))
                .thenReturn(aLoanResponse());

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 1L))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.inventoryNumber").isEqualTo("BC-000001");
    }

    @Test
    void getLoan_noLoanWithId_returns404() {
        when(loanService.findById(1L))
                .thenThrow(LoanExceptions.LoanNotFoundException.class);

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 1L))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void lendCopy_returnsLocationAnd201() {
        when(loanService.lend(any()))
                .thenReturn(aLoanResponse());

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "memberId": "1",
                            "inventoryNumber": "BC-000001"
                        }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .headers()
                .containsHeader("Location");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void lendCopy_invalidRequest_returns400(String description, String json, String field) {
        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errors." + field, val -> assertThat(val).isNotNull());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("memberId null", """
                        {"memberId": null, "inventoryNumber": "BC-000001" }""", "memberId"),
                Arguments.of("memberId negative", """
                        {"memberId": "-2", "inventoryNumber": "BC-000001" }""", "memberId"),
                Arguments.of("inventoryNumber null", """
                        {"memberId": "20", "inventoryNumber": null }""", "inventoryNumber"),
                Arguments.of("inventoryNumber blank", """
                        {"memberId": "20", "inventoryNumber": "  " }""", "inventoryNumber")
                );
    }

    @ParameterizedTest(name = "{0} is unknown")
    @MethodSource("exceptionProvider")
    void lendCopy_unknownMember_returns404(String missing, Class<? extends Exception> exception) {
        when(loanService.lend(any()))
                .thenThrow(exception);

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "memberId": "1",
                            "inventoryNumber": "BC-000001"
                         }
                        """))
                .hasStatus(HttpStatus.NOT_FOUND);

    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of("member", MemberExceptions.MemberNotFoundException.class),
                Arguments.of("bookCopy", BookCopyExceptions.BookCopyNotFoundException.class)
        );
    }

    @ParameterizedTest(name = "{0} → 409")
    @MethodSource("conflictCases")
    void lendCopy_conflict_returns409(String reason, RuntimeException exception, String type) {
        when(loanService.lend(any())).thenThrow(exception);

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"memberId": 1, "inventoryNumber": "BC-000001"}
                    """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://library.example/problems/" + type);
    }

    static Stream<Arguments> conflictCases() {
        return Stream.of(
                Arguments.of("limit reached",
                        new LoanExceptions.LoanLimitExceededException(1L, 5L), "loan-limit-exceeded"),
                Arguments.of("copy not available",
                        new BookCopyExceptions.CopyNotAvailableException(1L, "BC-000001"), "copy-not-available")
        );
    }

    private static final LocalDate LOANED   = LocalDate.of(2026, 7, 10);
    private static final LocalDate DUE      = LocalDate.of(2026, 7, 24);
    private static final LocalDate RETURNED = LocalDate.of(2026, 7, 27);

    @Test
    void returnCopy_openLoan_returns200WithLoan() {
        when(loanService.returnLoan(7L)).thenReturn(new LoanDtos.LoanResponse(
                7L, 1L, "BC-000001", LOANED, DUE, RETURNED, 3L));

        assertThat(mvc.post().uri(BASE_URI + "/{id}/return", 7))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                    {
                      "id": 7,
                      "inventoryNumber": "BC-000001",
                      "dueAt": "2026-07-24",
                      "returnedAt": "2026-07-27",
                      "daysOverdue": 3
                    }
                    """);

        verify(loanService).returnLoan(7L);
    }

    @Test
    void returnCopy_unknownLoan_returns404() {
        when(loanService.returnLoan(99L)).thenThrow(new LoanExceptions.LoanNotFoundException(99L));

        assertThat(mvc.post().uri(BASE_URI + "/{id}/return", 99))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://library.example/problems/not-found");
    }

    @ParameterizedTest(name = "{0} → 409")
    @MethodSource("returnConflictCases")
    void returnCopy_conflict_returns409(String reason, RuntimeException exception, String type) {
        when(loanService.returnLoan(7L)).thenThrow(exception);

        assertThat(mvc.post().uri(BASE_URI + "/{id}/return", 7))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://library.example/problems/" + type);
    }

    static Stream<Arguments> returnConflictCases() {
        return Stream.of(
                Arguments.of("already returned",
                        new LoanExceptions.CopyAlreadyReturnedException(7L), "copy-already-returned"),
                Arguments.of("copy not loaned",
                        new BookCopyExceptions.CopyNotAvailableException(1L, "BC-000001"), "copy-not-available")
        );
    }

    @Test
    void returnCopy_nonNumericId_returns400() {
        assertThat(mvc.post().uri(BASE_URI + "/abc/return"))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(loanService);
    }

    private static LoanDtos.LoanResponse aLoanResponse() {
        return new LoanDtos.LoanResponse(
                1L,
                2L,
                "BC-000001",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1),
                null,
                0L
        );
    }
}
