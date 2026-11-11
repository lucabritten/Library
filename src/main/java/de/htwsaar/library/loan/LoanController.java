package de.htwsaar.library.loan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Loans", description = "Add and delete loans")
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Lend a book-copy")
    @ApiResponse(responseCode = "201", description = "resource created successfully")
    @ApiResponse(responseCode = "404", description = "Did not found requested resource")
    @ApiResponse(responseCode = "409", description = "Copy already loaned or not available")
    @PostMapping
    public ResponseEntity<LoanDtos.LoanResponse> lendCopy(@Valid @RequestBody LoanDtos.CreateLoanRequest req) {
        LoanDtos.LoanResponse response = loanService.lend(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @Operation(summary = "Get a requested loan")
    @ApiResponse(responseCode = "404", description = "Requested loan not found.")
    @GetMapping("/{id}")
    public ResponseEntity<LoanDtos.LoanResponse> getLoan(@PathVariable Long id) {
        LoanDtos.LoanResponse requested = loanService.findById(id);

        return ResponseEntity.ok(requested);
    }

    @Operation(summary = "Return a book-copy")
    @ApiResponse(responseCode = "200", description = "Copy returned successfully")
    @ApiResponse(responseCode = "404", description = "Did not find requested resource.")
    @ApiResponse(responseCode = "409", description = "Conflict with used resource")
    @PostMapping("/{id}/return")
    public ResponseEntity<LoanDtos.LoanResponse> returnCopy(@PathVariable Long id) {
        LoanDtos.LoanResponse returned = loanService.returnLoan(id);

        return ResponseEntity.ok(returned);
    }
}
