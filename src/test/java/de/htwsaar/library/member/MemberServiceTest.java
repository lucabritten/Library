package de.htwsaar.library.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    MemberRepo memberRepo;

    @InjectMocks
    MemberService memberService;

    private static MemberDtos.CreateMemberRequest createMemberRequest(String email) {
        return new MemberDtos.CreateMemberRequest("Testuser", email);
    }

    @Test
    void require_noMemberWithId_throws() {
        when(memberRepo.findById(123L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.require(123L))
                .isInstanceOf(MemberExceptions.MemberNotFoundException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "'EXAMPLE@Test.com  ', example@test.com",
            "' EXAMPLE@test.com ', example@test.com",
            "'  example@test.com  ', example@test.com",
            "example@test.com, example@test.com"
    })
    void create_normalizesEmail(String input, String expected) {
        when(memberRepo.existsByEmail(any()))
                .thenReturn(false);
        when(memberRepo.saveAndFlush(any(Member.class))).then(AdditionalAnswers.returnsFirstArg());
        MemberDtos.CreateMemberRequest request = createMemberRequest(input);

        MemberDtos.MemberResponse response = memberService.create(request);
        assertThat(response.email()).isEqualTo(expected);
    }

    @Test
    void create_memberWithEmailExists_throwsAlreadyExists() {
        when(memberRepo.existsByEmail("example@test.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.create(createMemberRequest(" EXAMPLE@Test.com ")))
                .isInstanceOf(MemberExceptions.MemberAlreadyExistsException.class);

        verify(memberRepo, never()).saveAndFlush(any());
    }
}
