package com.library.bookservice.controller;

import com.library.bookservice.dto.AvailabilityResponse;
import com.library.bookservice.dto.BookDto;
import com.library.bookservice.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(bookService.search(search));
        }
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping
    public ResponseEntity<BookDto> createBook(@Valid @RequestBody BookDto bookDto) {
        BookDto created = bookService.createBook(bookDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(@PathVariable Long id, @Valid @RequestBody BookDto bookDto) {
        return ResponseEntity.ok(bookService.updateBook(id, bookDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Internal endpoints called by loan-service via Feign ----

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.checkAvailability(id));
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<AvailabilityResponse> reserveCopy(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.reserveCopy(id));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<AvailabilityResponse> releaseCopy(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.releaseCopy(id));
    }
}
