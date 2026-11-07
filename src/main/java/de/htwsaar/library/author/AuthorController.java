package de.htwsaar.library.author;

import de.htwsaar.library.author.AuthorDtos.*;
import de.htwsaar.library.common.Dtos.PageResponse;

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

@Tag(name = "Authors", description = "Manage and search for authors")
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @ApiResponse(responseCode = "200", description = "successfull fetched author.")
    @ApiResponse(responseCode = "404", description = "author not found.")
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getById(@PathVariable Long id) {
        AuthorResponse response = authorService.findById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a ,optionally by lastName filtered, page of authors")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "200", description = "Successful request")
    @GetMapping
    public ResponseEntity<PageResponse<AuthorResponse>> search(
            @RequestParam(required = false) String lastName,
            @PageableDefault(size = 10, sort = "lastName") Pageable pageable
            ) {
        PageResponse<AuthorResponse> authors = authorService.search(lastName, pageable);

        return ResponseEntity.ok(authors);
    }


    @Operation(summary = "Add an author")
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody AuthorDtos.CreateAuthorRequest req) {
        AuthorResponse createdAuthor = authorService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdAuthor.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdAuthor);
    }


    @Operation(summary = "Delete author")
    @ApiResponse(responseCode = "409", description = "Author is still connected to other entities in DB.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        authorService.delete(id);
    }
}
