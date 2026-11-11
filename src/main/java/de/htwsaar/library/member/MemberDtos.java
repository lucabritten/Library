package de.htwsaar.library.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberDtos {

    public record CreateMemberRequest(

            @NotBlank(message = "Name cannot be blank")
            @Size(max = 200)
            String name,

            @NotBlank(message = "Email cannot be blank")
            @Email
            @Size(max = 320)
            String email
    ) {}

    public record MemberResponse(
            Long id,
            String name,
            String email
    ) {
        public static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getId(),
                    member.getName(),
                    member.getEmail()
            );
        }
    }
}
