package de.htwsaar.library.auth;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Login and token issuing")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Exchange email and password for an access token")
    @ApiResponse(responseCode = "200", description = "Token issued")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/token")
    public TokenDtos.TokenResponse token(@Valid @RequestBody TokenDtos.LoginRequest req) {
        return authService.login(req);
    }
}
