package com.library.loanservice.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
