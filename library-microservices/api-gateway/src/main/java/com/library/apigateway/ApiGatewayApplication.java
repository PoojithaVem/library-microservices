package com.library.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for all clients (Postman / frontend).
 * Routes requests to book-service, member-service, and loan-service
 * using Eureka service discovery + client-side load balancing
 * (lb://service-name resolves to a healthy instance at request time).
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
