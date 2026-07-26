package com.library.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** What GET /api/notifications returns - the human-readable result of processing a LoanEvent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String eventId;
    private Long memberId;
    private String message;
    private LocalDateTime sentAt;
}
