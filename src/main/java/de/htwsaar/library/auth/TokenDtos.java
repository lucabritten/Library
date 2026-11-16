package de.htwsaar.library.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TokenDtos {

    record LoginRequest(

            @NotBlank
            @Email
            String email,

            @Size(max = 72)
            String password
    ) {}

    record TokenResponse(String jwtToken, String authType, long ttlSeconds) {}
}
