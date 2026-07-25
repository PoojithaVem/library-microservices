package com.library.bookservice.exception;

/** Thrown when loan-service tries to borrow a book with zero available copies. */
public class BookUnavailableException extends RuntimeException {
    public BookUnavailableException(String message) {
        super(message);
    }
}
