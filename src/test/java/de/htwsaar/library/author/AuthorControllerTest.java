package de.htwsaar.library.author;

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


@WebMvcTest(AuthorController.class)
public class AuthorControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    AuthorService authorService;

    private final static String BASE_URI = "/api/v1/authors";


    @Test
    void getById_returnsAuthorAsJson() {
        when(authorService.findById(42L))
                .thenReturn(new AuthorDtos.AuthorResponse(42L, "Karl", "Heinz"));

        assertThat(mvc.get().uri(BASE_URI + "/42"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.lastName").isEqualTo("Heinz");
    }

    @Test
    void getById_noAuthorWithId_returns404() {
        when(authorService.findById(42L))
                .thenThrow(AuthorExceptions.AuthorNotFoundException.class);

        assertThat(mvc.get().uri(BASE_URI + "/42"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_returnsNoContentWhenSuccess() {
        assertThat(mvc.delete().uri(BASE_URI + "/42"))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void delete_authorInUse_returns409() {
        doThrow(AuthorExceptions.AuthorInUseException.class)
                .when(authorService).delete(42L);

        assertThat(mvc.delete().uri(BASE_URI + "/42"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void create_returnsLocationAnd201() {
        when(authorService.create(any()))
                .thenReturn(new AuthorDtos.AuthorResponse(42L, "Karl", "Heinz"));

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstName": "Karl",
                            "lastName": "Heinz"
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

        verify(authorService, never()).create(any());
    }

    static Stream<Arguments> invalidRequests() {
        String tooLong = "x".repeat(201);
        return Stream.of(
                Arguments.of("firstName leer",    """
                    {"firstName": "", "lastName": "Ende"}""", "firstName"),
                Arguments.of("firs,tName fehlt",   """
                    {"lastName": "Ende"}""", "firstName"),
                Arguments.of("lastName nur Leerzeichen", """
                    {"firstName": "Michael", "lastName": "   "}""", "lastName"),
                Arguments.of("lastName zu lang",  """
                    {"firstName": "Michael", "lastName": "%s"}""".formatted(tooLong), "lastName")
        );
    }

    @Test
    void search_withParams_passesFilterAndPageable() {
        when(authorService.search(any(), any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI + "?lastName=tolk&page=2&size=5&sort=firstName,desc"))
                .hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(authorService).search(eq("tolk"), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "firstName"));
    }

    @Test
    void search_withoutParams_usesDefaults() {
        when(authorService.search(any(), any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI)).hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(authorService).search(isNull(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("lastName"));
    }

    @Test
    void search_hugePageSize_isCappedAt100() {
        when(authorService.search(any(), any())).thenReturn(emptyPage());

        assertThat(mvc.get().uri(BASE_URI + "?size=5000")).hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(authorService).search(any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void search_returnsPageAsJson() {
        var page = new Dtos.PageResponse<>(
                List.of(new AuthorDtos.AuthorResponse(1L, "J.R.R.", "Tolkien")),
                0, 10, 1, 1);
        when(authorService.search(any(), any())).thenReturn(page);

        assertThat(mvc.get().uri(BASE_URI))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                    {
                      "content": [ { "id": 1, "lastName": "Tolkien" } ],
                      "page": 0,
                      "totalElements": 1,
                      "totalPages": 1
                    }
                    """);
    }

    private static Dtos.PageResponse<AuthorDtos.AuthorResponse> emptyPage() {
        return new Dtos.PageResponse<>(List.of(), 0, 10, 0, 0);
    }

}

