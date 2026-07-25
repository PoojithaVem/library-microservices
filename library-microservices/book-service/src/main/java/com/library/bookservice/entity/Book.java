package com.library.bookservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
    @SequenceGenerator(name = "book_seq", sequenceName = "book_id_seq", allocationSize = 20)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    // Kept separate from totalCopies rather than computed, so a single UPDATE
    // (guarded by an optimistic @Version check) can safely decrement/increment it
    // when loan-service calls in to reserve/release a copy.
    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    @Version
    private Long version;
}
