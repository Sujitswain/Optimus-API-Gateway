package com.sujit.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		// Using WebFlux/reactive stack instead of virtual-thread servlet stack.
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}

/*
// SERVLET/VIRTUAL THREAD IMPLEMENTATION
// Previously used blocking servlet filters with virtual threads,
// but the current implementation is reactive WebFlux with Reactor.
*/
