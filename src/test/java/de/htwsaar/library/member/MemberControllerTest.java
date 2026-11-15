package de.htwsaar.library.member;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    private static final String BASE_URI = "/api/v1/members";

    @Autowired MockMvcTester mvc;
    @MockitoBean MemberService memberService;

    @Test
    void getById_existingMember_returnsMemberAsJson() {
        when(memberService.findById(7L))
                .thenReturn(new MemberDtos.MemberResponse(7L, "Alice", "alice@example.com"));

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 7))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"id": 7, "name": "Alice", "email": "alice@example.com"}
                        """);
    }

    @Test
    void getById_unknownMember_returns404() {
        when(memberService.findById(99L))
                .thenThrow(new MemberExceptions.MemberNotFoundException(99L));

        assertThat(mvc.get().uri(BASE_URI + "/{id}", 99))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://library.example/problems/not-found");
    }

    @Test
    void getById_nonNumericId_returns400() {
        assertThat(mvc.get().uri(BASE_URI + "/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(memberService);
    }

    @Test
    void create_validRequest_returns201WithLocation() {
        when(memberService.create(any()))
                .thenReturn(new MemberDtos.MemberResponse(7L, "Alice", "alice@example.com"));

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Alice", "email": "alice@example.com"}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "http://localhost/api/v1/members/7")
                .bodyJson()
                .extractingPath("$.id").isEqualTo(7);

        verify(memberService).create(new MemberDtos.CreateMemberRequest("Alice", "alice@example.com"));
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

        verify(memberService, never()).create(any());
    }

    static Stream<Arguments> invalidRequests() {
        String tooLongName = "x".repeat(201);
        String tooLongEmail = "a".repeat(310) + "@example.com";   // 322 Zeichen
        return Stream.of(
                Arguments.of("name leer", """
                        {"name": "", "email": "alice@example.com"}""", "name"),
                Arguments.of("name fehlt", """
                        {"email": "alice@example.com"}""", "name"),
                Arguments.of("name zu lang", """
                        {"name": "%s", "email": "alice@example.com"}""".formatted(tooLongName), "name"),
                Arguments.of("email leer", """
                        {"name": "Alice", "email": ""}""", "email"),
                Arguments.of("email fehlt", """
                        {"name": "Alice"}""", "email"),
                Arguments.of("email ohne @", """
                        {"name": "Alice", "email": "alice.example.com"}""", "email"),
                Arguments.of("email zu lang", """
                        {"name": "Alice", "email": "%s"}""".formatted(tooLongEmail), "email")
        );
    }

    @Test
    void create_emailAlreadyExists_returns409() {
        when(memberService.create(any()))
                .thenThrow(new MemberExceptions.MemberAlreadyExistsException("alice@example.com"));

        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Alice", "email": "alice@example.com"}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://library.example/problems/member-already-exists");
    }

    @Test
    void create_malformedJson_returns400() {
        assertThat(mvc.post().uri(BASE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Alice\""))   // schließende Klammer fehlt
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(memberService);
    }

    @Test
    void delete_existingMember_returns204() {
        assertThat(mvc.delete().uri(BASE_URI + "/{id}", 7))
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(memberService).deleteById(7L);
    }
}