package de.htwsaar.library.member;

import de.htwsaar.library.RepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MemberRepoTest extends RepositoryTest {

    @Autowired
    MemberRepo repo;

    @Test
    void save_duplicateEmail_throwsDataIntegrityViolation() {
        String email = "bernd@capitol.com";
        given(new Member("Bernd", email, "password_hash"));
        Member duplicate = new Member("Bernd-clon", email, "password_hash");

        assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
