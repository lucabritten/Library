package de.htwsaar.library.loan;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface LoanRepo extends JpaRepository<Loan, Long> {

    long countByMemberIdAndReturnedAtIsNull(Long memberId);

    @EntityGraph(attributePaths = "copy")
    Optional<Loan> findById(Long id);

    @EntityGraph(attributePaths = "copy")
    Optional<Loan> findByIdAndMemberId(Long id, Long memberId);
}
