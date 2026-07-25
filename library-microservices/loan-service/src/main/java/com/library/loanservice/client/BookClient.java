package com.library.loanservice.client;

import com.library.loanservice.dto.BookAvailabilityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Declarative HTTP client for book-service.
 *
 * "name" (not a hardcoded URL) + Eureka + spring-cloud-loadbalancer means this
 * call is resolved to a healthy book-service instance at request time - no
 * hardcoded host/port anywhere, and it transparently load-balances if there
 * are multiple book-service replicas.
 *
 * fallbackFactory backs this with a circuit breaker (see application.yml,
 * resilience4j.circuitbreaker config) - if book-service is down or the
 * breaker is open, calls short-circuit to BookClientFallbackFactory instead
 * of hanging every loan request.
 */
@FeignClient(name = "book-service", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient {

    @GetMapping("/api/books/{id}/availability")
    BookAvailabilityDto checkAvailability(@PathVariable("id") Long bookId);

    @PostMapping("/api/books/{id}/reserve")
    BookAvailabilityDto reserveCopy(@PathVariable("id") Long bookId);

    @PostMapping("/api/books/{id}/release")
    BookAvailabilityDto releaseCopy(@PathVariable("id") Long bookId);
}
