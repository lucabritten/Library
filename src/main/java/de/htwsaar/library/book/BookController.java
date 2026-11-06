package de.htwsaar.library.book;

import de.htwsaar.library.book.BookDtos.BookResponse;
import de.htwsaar.library.book.BookDtos.PageResponse;

import de.htwsaar.library.book.BookDtos.CreateBookRequest;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.net.URI;


@Tag(name = "Books", description = "Manage and search the book-catalog") // Swagger-UI zusätzliche Infos
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        BookResponse response = bookService.findById(id);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get a filtered page of books")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> search(
            @RequestParam(required = false) String author,
            @PageableDefault(size = 20, sort= "title") Pageable pageable
            ) {
            PageResponse<BookResponse> books = bookService.search(author, pageable);

            return ResponseEntity.ok(books);
    }


    @Operation(summary = "Add book to catalog") //Swagger-UI Doku
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "409", description = "ISBN already exists") //Swagger-UI Doku
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest req) {
        BookResponse createdBook = bookService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBook.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdBook);
    }


    @Operation(summary = "Delete book from catalog by id")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }
}
