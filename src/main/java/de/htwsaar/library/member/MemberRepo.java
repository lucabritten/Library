package de.htwsaar.library.member;

import org.springframework.data.jpa.repository.JpaRepository;

interface MemberRepo extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
}
