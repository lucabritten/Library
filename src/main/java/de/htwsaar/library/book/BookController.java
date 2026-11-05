package de.htwsaar.library.book;

import de.htwsaar.library.book.BookDtos.BookResponse;
import de.htwsaar.library.book.BookDtos.CreateBookRequest;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest req) {
        BookResponse createdBook = bookService.create(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdBook);
    }
}
