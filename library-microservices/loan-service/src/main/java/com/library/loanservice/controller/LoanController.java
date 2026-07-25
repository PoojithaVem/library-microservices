package com.library.loanservice.controller;

import com.library.loanservice.dto.LoanDto;
import com.library.loanservice.dto.LoanRequest;
import com.library.loanservice.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/borrow")
    public ResponseEntity<LoanDto> borrowBook(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrowBook(request));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<LoanDto> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanDto> getLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<LoanDto>> getLoansForMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(loanService.getLoansForMember(memberId));
    }

    // Paginated variant - demonstrates handling a potentially large result set
    // via Pageable rather than returning a member's entire loan history at once.
    @GetMapping("/member/{memberId}/paged")
    public ResponseEntity<Page<LoanDto>> getLoansForMemberPaged(@PathVariable Long memberId, Pageable pageable) {
        return ResponseEntity.ok(loanService.getLoansForMemberPaged(memberId, pageable));
    }
}
