package com.library.notificationservice.service;

import com.library.notificationservice.dto.NotificationDto;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * In-memory notification store, deliberately NOT backed by a database.
 *
 * This is a simplification for the demo: a real notification-service would
 * persist to its own database (notification_db, keeping the database-per-
 * service pattern) or push straight out to an email/SMS/push provider.
 * Keeping it in-memory here avoids adding a 4th Postgres database just to
 * prove the Kafka wiring works - the interesting part of this service is
 * the consumer, not the storage.
 *
 * Bounded to the last 100 entries so it can't grow unbounded in a long-running
 * demo session - the same "don't load everything into memory" instinct as
 * the pagination discussion elsewhere in this project, just applied to an
 * in-process cache instead of a query.
 */
@Service
public class NotificationService {

    private static final int MAX_HISTORY = 100;

    private final Deque<NotificationDto> recent = new ArrayDeque<>();

    public synchronized void record(NotificationDto notification) {
        recent.addFirst(notification);
        while (recent.size() > MAX_HISTORY) {
            recent.removeLast();
        }
    }

    public synchronized List<NotificationDto> getRecent() {
        return Collections.unmodifiableList(List.copyOf(recent));
    }
}
