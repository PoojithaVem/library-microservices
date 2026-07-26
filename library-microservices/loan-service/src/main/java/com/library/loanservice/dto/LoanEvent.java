package com.library.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The message published to Kafka whenever a loan is created or returned.
 *
 * This is loan-service's PUBLIC event contract - notification-service (and
 * any future consumer: analytics, audit log, email service) depends on this
 * shape, not on loan-service's internal Loan entity. Changing this class is
 * effectively a breaking-change decision across services, same as changing
 * a REST DTO - just async instead of sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEvent implements Serializable {
    private String eventId;
    private Long loanId;
    private Long bookId;
    private Long memberId;
    private LoanEventType eventType;
    private LocalDateTime occurredAt;
}
