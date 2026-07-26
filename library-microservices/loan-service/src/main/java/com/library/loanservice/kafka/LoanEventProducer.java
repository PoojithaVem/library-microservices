package com.library.loanservice.kafka;

import com.library.loanservice.dto.LoanEvent;
import com.library.loanservice.dto.LoanEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publishes loan lifecycle events to Kafka.
 *
 * Deliberately fire-and-forget from the caller's perspective: LoanService
 * calls publish() AFTER the loan is already committed to loan_db, so a
 * Kafka outage never blocks or fails a borrow/return - it just means the
 * notification is delayed until Kafka (or the outbox relay, in a fuller
 * implementation) catches back up. This is the same reasoning as the Outbox
 * pattern discussed in interview prep, simplified here to a direct publish
 * for demo purposes - a real production system would write the event to an
 * outbox table in the SAME transaction as the loan, then have a separate
 * relay publish it, to guarantee the two never get out of sync.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventProducer {

    private final KafkaTemplate<String, LoanEvent> kafkaTemplate;

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    public void publishBorrowed(Long loanId, Long bookId, Long memberId) {
        publish(loanId, bookId, memberId, LoanEventType.BORROWED);
    }

    public void publishReturned(Long loanId, Long bookId, Long memberId) {
        publish(loanId, bookId, memberId, LoanEventType.RETURNED);
    }

    private void publish(Long loanId, Long bookId, Long memberId, LoanEventType type) {
        LoanEvent event = LoanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .loanId(loanId)
                .bookId(bookId)
                .memberId(memberId)
                .eventType(type)
                .occurredAt(LocalDateTime.now())
                .build();

        // Keyed by memberId (as a String) - Kafka hashes the key to pick a
        // partition, so every event for the same member always lands on the
        // same partition and is delivered to consumers IN ORDER relative to
        // each other. Different members can land on different partitions
        // and process in parallel.
        String key = String.valueOf(memberId);

        kafkaTemplate.send(loanEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} event for loan {}: {}", type, loanId, ex.getMessage());
                    } else {
                        log.info("Published {} event for loan {} to partition {}",
                                type, loanId, result.getRecordMetadata().partition());
                    }
                });
    }
}
