package com.library.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirrors loan-service's LoanEvent - this is the CONSUMER's copy of the
 * event contract. Deliberately duplicated rather than shared via a common
 * library module: each service owning its own copy of the DTOs it depends
 * on is consistent with the same "no shared code between services" principle
 * used for the Feign DTOs (BookAvailabilityDto, MemberDto) elsewhere in this
 * project. A shared library would couple both services' deploy schedules
 * together - exactly what microservices are trying to avoid.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEvent {
    private String eventId;
    private Long loanId;
    private Long bookId;
    private Long memberId;
    private LoanEventType eventType;
    private LocalDateTime occurredAt;
}
