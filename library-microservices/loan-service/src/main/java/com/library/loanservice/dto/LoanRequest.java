package com.library.loanservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanRequest {
    @NotNull(message = "bookId is required")
    private Long bookId;

    @NotNull(message = "memberId is required")
    private Long memberId;
}
