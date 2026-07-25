package com.library.bookservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight response used by loan-service (via Feign) to check/reserve a copy. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Long bookId;
    private boolean available;
    private int availableCopies;
}
