package com.library.notificationservice.controller;

import com.library.notificationservice.dto.NotificationDto;
import com.library.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only view into what the consumer has processed - purely for demoing
 * that the Kafka pipeline actually works end to end (borrow -> event ->
 * this list updates, with no direct call from loan-service to this endpoint).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getRecent() {
        return ResponseEntity.ok(notificationService.getRecent());
    }
}
