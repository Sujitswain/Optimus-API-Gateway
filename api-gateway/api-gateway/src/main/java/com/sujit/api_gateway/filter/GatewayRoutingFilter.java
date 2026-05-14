package com.sujit.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class GatewayRoutingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingFilter.class);

    private final WebClient webClient = WebClient.builder().build();

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
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestURI = exchange.getRequest().getURI().getPath();
        String targetBaseUrl = findTargetUrl(requestURI);
        if (targetBaseUrl == null) {
            return chain.filter(exchange);
        }

        String targetUrl = targetBaseUrl + requestURI.substring(4); // Remove /api prefix
        HttpMethod method = exchange.getRequest().getMethod();

        WebClient.RequestBodySpec requestSpec = webClient.method(method)
                .uri(targetUrl);

        exchange.getRequest().getHeaders().forEach((name, values) -> values.forEach(value -> requestSpec.header(name, value)));

        Mono<ClientResponse> responseMono = (method == HttpMethod.GET || method == HttpMethod.DELETE)
                ? requestSpec.exchangeToMono(Mono::just)
                : requestSpec.body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody())).exchangeToMono(Mono::just);

        return responseMono
                .flatMap(clientResponse -> forwardResponse(clientResponse, exchange.getResponse()))
                .doOnSuccess(unused -> log.info("Routed {} {} -> {}", method, requestURI, targetUrl))
                .doOnError(e -> log.error("Error routing request to backend: {}", e.getMessage()));
    }

    private String findTargetUrl(String requestURI) {
        return ROUTES.entrySet().stream()
                .filter(entry -> requestURI.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Mono<Void> forwardResponse(ClientResponse clientResponse, ServerHttpResponse response) {
        response.setStatusCode(clientResponse.statusCode());
        clientResponse.headers().asHttpHeaders().forEach((name, values) -> values.forEach(value -> response.getHeaders().add(name, value)));
        return response.writeWith(clientResponse.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class));
    }
}

/*
// PREVIOUS SERVLET/VIRTUAL-THREAD IMPLEMENTATION
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.web.filter.OncePerRequestFilter;
// import java.io.IOException;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
//
// @Component
// public class GatewayRoutingFilter extends OncePerRequestFilter implements Ordered {
//
//     private static final Logger log = LoggerFactory.getLogger(GatewayRoutingFilter.class);
//
//     private final HttpClient httpClient = HttpClient.newBuilder()
//             .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
//             .build();
//
//     private static final Map<String, String> ROUTES = Map.of(
//             "/api/auth/", "http://localhost:8081",
//             "/api/orders/", "http://localhost:8082",
//             "/api/payments/", "http://localhost:8082",
//             "/api/products/", "http://localhost:8082"
//     );
//
//     @Override
//     public int getOrder() {
//         return Ordered.LOWEST_PRECEDENCE - 1;
//     }
//
//     @Override
//     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//             throws ServletException, IOException {
//
//         String requestURI = request.getRequestURI();
//
//         String targetUrl = findTargetUrl(requestURI);
//         if (targetUrl != null) {
//             routeToBackend(request, response, targetUrl);
//         } else {
//             filterChain.doFilter(request, response);
//         }
//     }
//
//     private String findTargetUrl(String requestURI) {
//         return ROUTES.entrySet().stream()
//                 .filter(entry -> requestURI.startsWith(entry.getKey()))
//                 .map(Map.Entry::getValue)
//                 .findFirst()
//                 .orElse(null);
//     }
//
//     private void routeToBackend(HttpServletRequest request, HttpServletResponse response, String targetBaseUrl)
//             throws IOException {
//
//         try {
//             String targetUrl = targetBaseUrl + request.getRequestURI().substring(4);
//             HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
//                     .uri(URI.create(targetUrl))
//                     .method(request.getMethod(), HttpRequest.BodyPublishers.noBody());
//
//             request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
//                 request.getHeaders(headerName).asIterator().forEachRemaining(headerValue -> {
//                     requestBuilder.header(headerName, headerValue);
//                 });
//             });
//
//             HttpResponse<String> backendResponse = httpClient.send(requestBuilder.build(),
//                     HttpResponse.BodyHandlers.ofString());
//
//             response.setStatus(backendResponse.statusCode());
//             backendResponse.headers().map().forEach((headerName, headerValues) -> {
//                 headerValues.forEach(headerValue -> response.setHeader(headerName, headerValue));
//             });
//             response.getWriter().write(backendResponse.body());
//
//             log.info("Routed {} {} -> {} (status: {})",
//                     request.getMethod(), request.getRequestURI(), targetUrl, backendResponse.statusCode());
//
//         } catch (Exception e) {
//             log.error("Error routing request to backend: {}", e.getMessage());
//             response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//             response.getWriter().write("{\"error\": \"Backend service unavailable\"}");
//         }
//     }
// }
*/