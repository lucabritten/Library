package de.htwsaar.library.copy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/copies")
public class BookCopyController {

    private final BookCopyService bookCopyService;

    public BookCopyController(BookCopyService bookCopyService) {
        this.bookCopyService = bookCopyService;
    }

    @Operation(summary = "Get a book-copy by inventory-number")
    @ApiResponse(responseCode = "404", description = "Book copy with requested inventory-number not found.")
    @GetMapping("/{inventoryNumber}")
    public ResponseEntity<BookCopyDto.BookCopyResponse> getByInventoryNumber(@PathVariable String inventoryNumber) {
        BookCopyDto.BookCopyResponse response = bookCopyService.findByInventoryNumber(inventoryNumber);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Add book-copy to catalog")
    @ApiResponse(responseCode = "201", description = "Book-copy added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid field input")
    @ApiResponse(responseCode = "404", description = "Related book not found")
    @PostMapping
    public ResponseEntity<BookCopyDto.BookCopyResponse> create(@Valid @RequestBody BookCopyDto.CreateBookCopyRequest req) {
        BookCopyDto.BookCopyResponse created = bookCopyService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.inventoryNumber())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(created);
    }

    @Operation(summary = "Withdraw copy from catalog by inventory-number")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "409", description = "Copy is not available.")
    @PutMapping("/{inventoryNumber}")
    public void withdrawCopy(@PathVariable String inventoryNumber) {
        bookCopyService.withdrawCopy(inventoryNumber);
    }
}
