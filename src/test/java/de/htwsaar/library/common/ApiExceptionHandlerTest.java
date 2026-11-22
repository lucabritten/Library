package de.htwsaar.library.common;

import de.htwsaar.library.book.BookController;
import de.htwsaar.library.book.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(BookController.class)
@ExtendWith(MockitoExtension.class)
@Import(SecurityConfig.class)
public class ApiExceptionHandlerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    BookService service;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void unexpectedException_returns500WithTraceIdAndNoInternals() {
        when(service.findById(100L))
                .thenThrow(new IllegalStateException("SELECT * FROM members failed"));

        assertThat(mvc.get().uri("/api/v1/books/100"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson()
                .hasPathSatisfying("$.traceId", id -> assertThat(id).isNotNull())
                .extractingPath("$.detail").isEqualTo("An unexpected error occurred");

        assertThat(mvc.get().uri("/api/v1/books/42"))
                .bodyText()
                .doesNotContain("SELECT");
    }
}
