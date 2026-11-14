package de.htwsaar.library.book;

import de.htwsaar.library.author.AuthorDtos;
import de.htwsaar.library.common.Dtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(BookController.class)
public class BookControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    BookService bookService;

    private final static String BASE_URI = "/api/v1/books";

    @Test
    void getById_returnsBookAsJson() {
        when(bookService.findById(42L))
                .thenReturn(new BookDtos.BookResponse(42L, "Star Wars", "12334", new BookDtos.AuthorSummary(1L, "Walter")));

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 42L))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Star Wars");
    }

    @Test
    void getById_unknownBook_returns404() {
        when(bookService.findById(42L))
                .thenThrow(BookExceptions.BookNotFoundException.class);

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 42L))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_isSuccessfully_returns404() {
        assertThat(mvc.delete().uri(BASE_URI + "/{id}", 123L))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void delete_bookInUse_returns409() {
        doThrow(BookExceptions.BookInUseException.class)
                .when(bookService).delete(123L);

        assertThat(mvc.delete().uri(BASE_URI + "/{id}", 123L))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void create_returnsLocationAnd201() {
        when(bookService.create(any()))
                .thenReturn(new BookDtos.BookResponse(123L, "Minecraft", "9781617290459", new BookDtos.AuthorSummary(1L, "Walter")));

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Minecraft",
                            "isbn": "9781617290459",
                            "authorId": "1"
                        }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .headers()
                .containsHeader("Location");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void create_invalidRequest_returns400WithFieldError(String description, String json, String field) {
        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errors." + field, value -> assertThat(value).isNotNull());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(
                        "Titel leer", """
                                {"title": "", "isbn":  "9781617290459", "authorId": "1"}""", "title"),
                Arguments.of(
                        "Titel fehlt", """
                        {"isbn":  "9781617290459", "authorId": "1"}""", "title"),
                Arguments.of(
                        "Invalid ISBN", """
                                {"title": "Star Wars", "isbn":  "9781607290459", "authorId": "1"}""", "isbn"),
                Arguments.of(
                        "Leere ISBN", """
                                {"title": "Star Wars", "isbn":  "", "authorId": "1"}""", "isbn"),
                Arguments.of(
                        "Negative authorId", """
                                {"title": "Star Wars", "isbn":  "", "authorId": "-1"}""", "authorId"),
                Arguments.of(
                        "authorId null", """
                                {"title": "Star Wars", "isbn":  "", "authorId": "null"}""", "authorId")
                );
    }

    @Test
    void create_isbnAlreadyExists_returns409() {
        when(bookService.create(any()))
                .thenThrow(BookExceptions.DuplicateIsbnException.class);

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Minecraft",
                            "isbn": "9781617290459",
                            "authorId": "1"
                        }
                        """))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void search_withParams_passesFilterAndPageable() {
        when(bookService.search(any(),any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI + "?authorId=10&page=2&size=5&sort=title,desc"))
                .hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).search(eq(10L), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "title"));
    }

    @Test
    void search_withoutParams_usesDefaults() {
        when(bookService.search(any(),any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI)).hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).search(isNull(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("title"));
    }

    private static Dtos.PageResponse<BookDtos.BookResponse> emptyPage() {
        return new Dtos.PageResponse<>(List.of(), 0, 10, 0, 0);
    }

    @Test
    void search_hugePageSize_isCappedAt100() {
        when(bookService.search(any(),any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI + "?size=1000")).hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).search(any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void search_returnsPageAsJson() {
        var page = new Dtos.PageResponse<>(
                List.of(new BookDtos.BookResponse(1L, "Minecraft", "9781617290459", new BookDtos.AuthorSummary(2L, "Walter"))),
                0, 10, 1, 1);
        when(bookService.search(any(), any())).thenReturn(page);

        assertThat(mvc.get().uri(BASE_URI))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                    {
                      "content": [ {"id":1,"title":"Minecraft","isbn":"9781617290459","author":{"id":2,"name":"Walter"}} ],
                      "page": 0,
                      "totalElements": 1,
                      "totalPages": 1
                    }
                    """);
    }
}
