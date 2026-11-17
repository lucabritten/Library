package de.htwsaar.library.loan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Protected endpoint. Authentication token required.")
    @ApiResponse(responseCode = "404", description = "Did not found  a requested resource like member or copy")
    @ApiResponse(responseCode = "409", description = "Copy already loaned or not available")
    @PostMapping
    public ResponseEntity<LoanDtos.LoanResponse> lendCopy(JwtAuthenticationToken auth,
                                                          @Valid @RequestBody LoanDtos.CreateLoanRequest req) {

        long memberId = memberId(auth);

        LoanDtos.LoanResponse response = loanService.lend(memberId, req);

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
    @ApiResponse(responseCode = "401", description = "Protected endpoint. Authentication token required.")
    @ApiResponse(responseCode = "403", description = "Privileges are not sufficient to use this endpoint. You are only allowed to see your own loans.")
    @ApiResponse(responseCode = "404", description = "Requested loan not found.")
    @GetMapping("/{id}")
    public ResponseEntity<LoanDtos.LoanResponse> getLoan(@PathVariable Long id,
                                                         JwtAuthenticationToken auth) {;
        LoanDtos.LoanResponse requested = loanService.findById(id, memberId(auth), isAdmin(auth));

        return ResponseEntity.ok(requested);
    }

    @Operation(summary = "Return a book-copy")
    @ApiResponse(responseCode = "200", description = "Copy returned successfully")
    @ApiResponse(responseCode = "401", description = "Protected endpoint. Authentication token required.")
    @ApiResponse(responseCode = "403", description = "Privileges are not sufficient to use this endpoint. You can only return your own loans.")
    @ApiResponse(responseCode = "404", description = "Did not find requested resource.")
    @ApiResponse(responseCode = "409", description = "Conflict with used resource")
    @PostMapping("/{id}/return")
    public ResponseEntity<LoanDtos.LoanResponse> returnCopy(@PathVariable Long id,
                                                            JwtAuthenticationToken auth) {
        LoanDtos.LoanResponse returned = loanService.returnLoan(id, memberId(auth), isAdmin(auth));

        return ResponseEntity.ok(returned);
    }

    private static long memberId(JwtAuthenticationToken auth) {
        return Long.parseLong(auth.getToken().getSubject());
    }

    private static boolean isAdmin(JwtAuthenticationToken auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_admin"));
    }
}
