package de.htwsaar.library.copy;

import de.htwsaar.library.book.BookExceptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@WebMvcTest(BookCopyController.class)
public class BookCopyControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    BookCopyService copyService;

    private final String BASE_URI = "/api/v1/copies";

    @Test
    void getByInventoryNumber_returnsCopyAsJson() {
        when(copyService.findByInventoryNumber("BC-000001"))
                .thenReturn(sampleResponse());

        assertThat(mvc.get().uri(BASE_URI + "/{inventoryNumber}", "BC-000001"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.copyStatus").isEqualTo("AVAILABLE");
    }

    @Test
    void getByInventoryNumber_noCopyWithNumber_throwsNotFound() {
        when(copyService.findByInventoryNumber("BC-000001"))
                .thenThrow(BookCopyExceptions.BookCopyNotFoundException.class);

        assertThat(mvc.get().uri(BASE_URI + "/{inventoryNumber}", "BC-000001"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void withdrawCopy_availableCopy_returns204() {
        assertThat(mvc.put().uri(BASE_URI + "/{inventoryNumber}", "BC-000001"))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void withdrawCopy_unavailableCopy_returns409() {
        doThrow(BookCopyExceptions.CopyNotAvailableException.class)
                .when(copyService).withdrawCopy("BC-000001");

        assertThat(mvc.put().uri(BASE_URI + "/{inventoryNumber}", "BC-000001"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void create_returnsLocationAnd201() {
        when(copyService.create(any()))
                .thenReturn(sampleResponse());

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "bookId": "1"
                        }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .headers()
                .containsHeader("Location");
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void create_invalidRequest_returns400(String description, String json, String field) {
        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errors." + field, val -> assertThat(val).isNotNull());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("BookId null", """
                        {"bookId": null}""", "bookId"),
                        Arguments.of("BookId negative", """
                        {"bookId": "-20"}""", "bookId")
        );
    }

    @Test
    void create_unknownBookId_returns404() {
        when(copyService.create(any()))
                .thenThrow(BookExceptions.BookNotFoundException.class);

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "bookId": "1"
                        }
                        """))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private static BookCopyDto.BookCopyResponse sampleResponse() {
        return new BookCopyDto.BookCopyResponse(1L, new BookCopyDto.BookSummary("Minecraft", "1234"), "BC-000001", CopyStatus.AVAILABLE);
    }

}
