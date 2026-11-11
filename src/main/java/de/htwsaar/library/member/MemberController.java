package de.htwsaar.library.member;

import de.htwsaar.library.member.MemberDtos.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "Get a member by id")
    @ApiResponse(responseCode = "404", description = "Member with id does not exist")
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getById(@PathVariable Long id) {
        MemberResponse response = memberService.findById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add member")
    @ApiResponse(responseCode = "201", description = "Created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberDtos.CreateMemberRequest req) {
        MemberResponse createdMember = memberService.create(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdMember.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdMember);
    }

    @Operation(summary = "Delete Member")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memberService.deleteById(id);
    }
}
