package com.library.loanservice.repository;

import com.library.loanservice.entity.Loan;
import com.library.loanservice.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status);

    // Paginated variant for a potentially large loan history -
    // avoids loading a member's entire lifetime of loans into memory at once.
    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    List<Loan> findByBookIdAndStatus(Long bookId, LoanStatus status);
}
