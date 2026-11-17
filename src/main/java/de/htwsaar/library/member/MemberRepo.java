package de.htwsaar.library.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface MemberRepo extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
