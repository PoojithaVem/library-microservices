package com.library.loanservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    /**
     * FULL logs every request/response including headers/body - noisy but
     * invaluable in a demo/interview to *show* the Feign call happening.
     * In real prod this would be BASIC or NONE to avoid logging PII/noise.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Forwards the caller's "Authorization: Bearer <jwt>" header onto every
     * outgoing Feign call.
     *
     * Without this, loan-service calls member-service/book-service as an
     * ANONYMOUS request - member-service's SecurityConfig correctly rejects
     * that with 403, since it has no idea who's asking. JWT identity does
     * NOT propagate automatically across service hops; each hop either
     * forwards the original token (what we do here) or the gateway mints a
     * new internal/service token - propagation is a deliberate design
     * decision, not something Spring gives you for free.
     */
    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
