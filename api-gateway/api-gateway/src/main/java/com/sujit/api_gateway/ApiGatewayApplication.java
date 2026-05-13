package com.sujit.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		// Enable virtual threads for better concurrency
		System.setProperty("spring.threads.virtual.enabled", "true");
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}

/*
// ORIGINAL WEBFLUX IMPLEMENTATION (commented out for reference)
// This was using Spring Cloud Gateway with WebFlux for reactive processing
// Now using traditional Spring Boot with virtual threads for simpler synchronous code
*/
