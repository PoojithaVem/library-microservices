package com.library.bookservice.service;

import com.library.bookservice.dto.AvailabilityResponse;
import com.library.bookservice.dto.BookDto;
import com.library.bookservice.entity.Book;
import com.library.bookservice.exception.BookUnavailableException;
import com.library.bookservice.exception.ResourceNotFoundException;
import com.library.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream().map(this::toDto).toList();
    }

    public BookDto getBookById(Long id) {
        return toDto(findOrThrow(id));
    }

    public List<BookDto> search(String keyword) {
        return bookRepository.searchByTitleOrAuthor(keyword).stream().map(this::toDto).toList();
    }

    @Transactional
    public BookDto createBook(BookDto dto) {
        if (bookRepository.existsByIsbn(dto.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN " + dto.getIsbn() + " already exists");
        }
        Book book = Book.builder()
                .isbn(dto.getIsbn())
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .totalCopies(dto.getTotalCopies())
                .availableCopies(dto.getTotalCopies()) // all copies start available
                .build();
        return toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto updateBook(Long id, BookDto dto) {
        Book book = findOrThrow(id);
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setTotalCopies(dto.getTotalCopies());
        // managed entity: no explicit save() needed, dirty checking flushes the UPDATE at commit
        return toDto(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id " + id);
        }
        bookRepository.deleteById(id);
    }

    /**
     * Called by loan-service (via Feign) when a member borrows a book.
     * @Version on the entity gives us optimistic locking: if two loan
     * requests race for the last copy, the second commit fails fast with
     * an OptimisticLockException instead of silently over-lending.
     */
    @Transactional
    public AvailabilityResponse reserveCopy(Long bookId) {
        Book book = findOrThrow(bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("No available copies for book id " + bookId);
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        return new AvailabilityResponse(book.getId(), true, book.getAvailableCopies());
    }

    /** Called by loan-service when a book is returned. */
    @Transactional
    public AvailabilityResponse releaseCopy(Long bookId) {
        Book book = findOrThrow(bookId);
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        return new AvailabilityResponse(book.getId(), true, book.getAvailableCopies());
    }

    public AvailabilityResponse checkAvailability(Long bookId) {
        Book book = findOrThrow(bookId);
        return new AvailabilityResponse(book.getId(), book.getAvailableCopies() > 0, book.getAvailableCopies());
    }

    private Book findOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
    }

    private BookDto toDto(Book book) {
        return BookDto.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .build();
    }
}
