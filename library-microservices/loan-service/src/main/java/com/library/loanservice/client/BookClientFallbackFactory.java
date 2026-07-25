package com.library.loanservice.client;

import com.library.loanservice.dto.BookAvailabilityDto;
import com.library.loanservice.exception.DownstreamServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FallbackFactory (rather than a plain Fallback) gives access to the
 * triggering exception - useful for logging *why* the circuit tripped
 * (timeout vs 5xx vs connection refused) instead of swallowing it silently.
 *
 * Design choice: book availability is core to a lending decision, so we
 * fail LOUD (a clear 503-style error) rather than guessing "available" -
 * a bad guess here could let two members borrow the last copy. Contrast
 * this with a read-only, non-critical call, where returning stale/default
 * data would be the better fallback.
 */
@Component
public class BookClientFallbackFactory implements FallbackFactory<BookClient> {

    private static final Logger log = LoggerFactory.getLogger(BookClientFallbackFactory.class);

    @Override
    public BookClient create(Throwable cause) {
        return new BookClient() {
            @Override
            public BookAvailabilityDto checkAvailability(Long bookId) {
                log.warn("book-service unavailable while checking book {}: {}", bookId, cause.getMessage());
                throw new DownstreamServiceException("Book service is currently unavailable. Please try again shortly.");
            }

            @Override
            public BookAvailabilityDto reserveCopy(Long bookId) {
                log.warn("book-service unavailable while reserving book {}: {}", bookId, cause.getMessage());
                throw new DownstreamServiceException("Book service is currently unavailable. Please try again shortly.");
            }

            @Override
            public BookAvailabilityDto releaseCopy(Long bookId) {
                log.warn("book-service unavailable while releasing book {}: {}", bookId, cause.getMessage());
                throw new DownstreamServiceException("Book service is currently unavailable. Please try again shortly.");
            }
        };
    }
}
