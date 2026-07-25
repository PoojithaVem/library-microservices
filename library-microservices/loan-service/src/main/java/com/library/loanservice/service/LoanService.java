package com.library.loanservice.service;

import com.library.loanservice.client.BookClient;
import com.library.loanservice.client.MemberClient;
import com.library.loanservice.dto.BookAvailabilityDto;
import com.library.loanservice.dto.LoanDto;
import com.library.loanservice.dto.LoanRequest;
import com.library.loanservice.entity.Loan;
import com.library.loanservice.entity.LoanStatus;
import com.library.loanservice.exception.BookUnavailableException;
import com.library.loanservice.exception.ResourceNotFoundException;
import com.library.loanservice.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final int LOAN_PERIOD_DAYS = 14;

    private final LoanRepository loanRepository;
    private final BookClient bookClient;     // Feign - calls book-service over HTTP via Eureka
    private final MemberClient memberClient; // Feign - calls member-service over HTTP via Eureka

    /**
     * Orchestrates a borrow across three services:
     *  1. member-service: confirm the member exists (via Feign)
     *  2. book-service: atomically reserve a copy (via Feign) - book-service's
     *     own @Version optimistic lock protects against a race on the last copy
     *  3. loan-service: record the loan locally
     *
     * If step 2 fails (BookUnavailableException from book-service, surfaced
     * through Feign as a client error), nothing has been written here yet -
     * so there's nothing to compensate. If we ever add a step AFTER the
     * reserve call that could itself fail, we'd need a compensating action
     * (call bookClient.releaseCopy) to undo the reservation - the Saga pattern,
     * just illustrated at small scale.
     */
    @Transactional
    public LoanDto borrowBook(LoanRequest request) {
        // 1. Validate the member exists (throws via fallback if member-service is down)
        memberClient.getMemberById(request.getMemberId());

        // 2. Reserve a copy - this is the operation that can legitimately fail
        //    (no copies left), distinct from a downstream-unavailable failure.
        BookAvailabilityDto reservation = bookClient.reserveCopy(request.getBookId());
        if (!reservation.isAvailable()) {
            throw new BookUnavailableException("No available copies for book id " + request.getBookId());
        }

        // 3. Record the loan
        Loan loan = Loan.builder()
                .bookId(request.getBookId())
                .memberId(request.getMemberId())
                .borrowDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(LOAN_PERIOD_DAYS))
                .status(LoanStatus.ACTIVE)
                .build();

        return toDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanDto returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            return toDto(loan); // idempotent: returning an already-returned loan is a no-op, not an error
        }

        bookClient.releaseCopy(loan.getBookId());

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);
        return toDto(loan);
    }

    public List<LoanDto> getLoansForMember(Long memberId) {
        return loanRepository.findByMemberId(memberId).stream().map(this::toDto).toList();
    }

    public Page<LoanDto> getLoansForMemberPaged(Long memberId, Pageable pageable) {
        return loanRepository.findByMemberId(memberId, pageable).map(this::toDto);
    }

    public LoanDto getLoanById(Long id) {
        return toDto(loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + id)));
    }

    private LoanDto toDto(Loan loan) {
        return LoanDto.builder()
                .id(loan.getId())
                .bookId(loan.getBookId())
                .memberId(loan.getMemberId())
                .borrowDate(loan.getBorrowDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus().name())
                .build();
    }
}
