package com.sujit.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Simple routing filter to replace Spring Cloud Gateway functionality
 * Routes requests to appropriate backend services using virtual threads
 */
@Component
public class GatewayRoutingFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingFilter.class);

    // Using virtual threads for HTTP client calls
    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
            .build();

    // TODO: Adding dummy service endpoints for now
    private static final Map<String, String> ROUTES = Map.of(
            "/api/auth/", "http://localhost:8081",
            "/api/orders/", "http://localhost:8082",
            "/api/payments/", "http://localhost:8082",
            "/api/products/", "http://localhost:8082"
    );

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Find matching route
        String targetUrl = findTargetUrl(requestURI);
        if (targetUrl != null) {
            // Route to backend service using virtual threads
            routeToBackend(request, response, targetUrl);
        } else {
            // Continue with normal processing (for actuator endpoints, etc.)
            filterChain.doFilter(request, response);
        }
    }

    private String findTargetUrl(String requestURI) {
        return ROUTES.entrySet().stream()
                .filter(entry -> requestURI.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private void routeToBackend(HttpServletRequest request, HttpServletResponse response, String targetBaseUrl)
            throws IOException {

        try {
            String targetUrl = targetBaseUrl + request.getRequestURI().substring(4); // Remove /api prefix

            // Build HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .method(request.getMethod(), HttpRequest.BodyPublishers.noBody());

            // Copy headers
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                request.getHeaders(headerName).asIterator().forEachRemaining(headerValue -> {
                    requestBuilder.header(headerName, headerValue);
                });
            });

            // Send request using virtual threads
            HttpResponse<String> backendResponse = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            // Copy response
            response.setStatus(backendResponse.statusCode());
            backendResponse.headers().map().forEach((headerName, headerValues) -> {
                headerValues.forEach(headerValue -> response.setHeader(headerName, headerValue));
            });
            response.getWriter().write(backendResponse.body());

            log.info("Routed {} {} -> {} (status: {})",
                    request.getMethod(), request.getRequestURI(), targetUrl, backendResponse.statusCode());

        } catch (Exception e) {
            log.error("Error routing request to backend: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Backend service unavailable\"}");
        }
    }
}

/*
// ORIGINAL SPRING CLOUD GATEWAY CONFIGURATION
// spring:
//   cloud:
//     gateway:
//       routes:
//         - id: auth-service
//           uri: http://localhost:8081
//           predicates:
//             - Path=/api/auth/**
//         - id: sample-backend
//           uri: http://localhost:8082
//           predicates:
//             - Path=/api/orders/**
//             - Path=/api/payments/**
//             - Path=/api/products/**
*/