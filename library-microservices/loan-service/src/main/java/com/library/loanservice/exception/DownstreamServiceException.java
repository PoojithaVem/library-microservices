package com.library.loanservice.exception;

/** Raised by a Feign fallback when a dependency (book-service/member-service) is unreachable. */
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message) {
        super(message);
    }
}
