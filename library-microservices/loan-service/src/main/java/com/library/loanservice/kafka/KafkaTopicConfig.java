package com.library.loanservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topic explicitly (rather than relying on Kafka's
 * auto.create.topics.enable) so the partition count and replication factor
 * are deliberate, versioned decisions - not an accident of whichever service
 * happens to publish first.
 *
 * 3 partitions: enough to show real parallelism in a demo without being
 * excessive. Messages are keyed by memberId (see LoanEventProducer), so all
 * events for one member always land on the same partition - order is
 * preserved per member, which is what actually matters here (you never need
 * "borrow" before "return" to be visible out of order for the SAME member).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(loanEventsTopic)
                .partitions(3)
                .replicas(1)   // single broker in this demo; real prod would use 3 for fault tolerance
                .build();
    }
}
