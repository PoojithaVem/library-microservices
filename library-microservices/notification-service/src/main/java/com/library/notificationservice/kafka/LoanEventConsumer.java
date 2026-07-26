package com.library.notificationservice.kafka;

import com.library.notificationservice.dto.LoanEvent;
import com.library.notificationservice.dto.NotificationDto;
import com.library.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes loan-events published by loan-service.
 *
 * groupId="notification-service" (set in application.yml) means: if we ever
 * scale this service to multiple replicas, Kafka splits the topic's
 * partitions across them automatically - each event is still processed by
 * exactly ONE instance in the group, not all of them. That's the actual
 * point of a consumer group, demonstrated here rather than just described.
 *
 * At-least-once delivery: this listener's ack happens AFTER processing
 * completes (default Spring Kafka behavior), so a crash mid-processing can
 * cause the same event to be redelivered on restart. record() below is
 * effectively idempotent in practice (re-adding the same notification just
 * duplicates a log line in this demo), but a real email/SMS send would need
 * an explicit dedup check keyed on eventId - the same idempotency principle
 * covered in interview prep, applied for real here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.loan-events:loan-events}", groupId = "notification-service")
    public void handleLoanEvent(LoanEvent event) {
        String message = switch (event.getEventType()) {
            case BORROWED -> "Member " + event.getMemberId() + " borrowed book " + event.getBookId()
                    + " (loan #" + event.getLoanId() + "). Due back in 14 days.";
            case RETURNED -> "Member " + event.getMemberId() + " returned book " + event.getBookId()
                    + " (loan #" + event.getLoanId() + "). Thank you!";
        };

        log.info("Processing loan event: {}", message);

        notificationService.record(NotificationDto.builder()
                .eventId(event.getEventId())
                .memberId(event.getMemberId())
                .message(message)
                .sentAt(event.getOccurredAt())
                .build());
    }
}
