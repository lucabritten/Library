package de.htwsaar.library.controller;


import de.htwsaar.library.model.Book;
import de.htwsaar.library.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookControllerImpl{

    private final BookService bookService;

    public BookControllerImpl(@Autowired BookService bookService){
        this.bookService = bookService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping // GET /books?author=MS%20Ebel
    public List<Book> getByAuthor(@RequestParam String author) {
        return bookService.findByAuthor(author);
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book book) {
        Book saved = bookService.save(book);
        return ResponseEntity.created(URI.create("/books/" +
                saved.getId())).body(saved);
    }
}
