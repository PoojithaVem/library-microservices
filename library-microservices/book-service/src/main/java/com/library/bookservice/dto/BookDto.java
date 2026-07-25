package com.library.bookservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO exposed at the API boundary instead of the JPA entity directly -
 * keeps the public contract stable even if the persistence model changes,
 * and avoids ever serializing lazy proxies or internal fields (like version).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {

    private Long id;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @Min(value = 0, message = "Total copies cannot be negative")
    private int totalCopies;

    private int availableCopies;
}
