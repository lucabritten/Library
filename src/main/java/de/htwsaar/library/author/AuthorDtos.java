package de.htwsaar.library.author;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthorDtos {

    public record CreateAuthorRequest(

            @NotBlank(message = "Firstname cannot be blank.")
            @Size(max = 100)
            String firstName,

            @Size(max = 200)
            @NotBlank(message = "Lastname cannot be blank.")
            String lastName

    ) {}

    public record AuthorResponse(Long id, String firstName, String lastName) {
        public static AuthorResponse from(Author author) {
            return new AuthorResponse(author.getId(), author.getFirstName(), author.getLastName());
        }
    }
}
