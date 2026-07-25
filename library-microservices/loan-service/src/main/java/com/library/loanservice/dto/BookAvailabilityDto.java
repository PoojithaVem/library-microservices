package com.library.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors book-service's AvailabilityResponse - the shape loan-service needs, nothing more. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookAvailabilityDto {
    private Long bookId;
    private boolean available;
    private int availableCopies;
}
