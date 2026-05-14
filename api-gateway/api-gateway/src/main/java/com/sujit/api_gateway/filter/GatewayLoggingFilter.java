package com.sujit.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GatewayLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long latency = System.currentTimeMillis() - start;
                    Integer statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : null;

                    log.info("[Gateway] {} {} -> {}ms status={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI(),
                            latency,
                            statusCode);
                });
    }
}

/*
// SERVLET-BASED IMPLEMENTATION
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.web.filter.OncePerRequestFilter;
// import java.io.IOException;
//
// @Component
// public class GatewayLoggingFilter extends OncePerRequestFilter implements Ordered {
//     private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);
//
//     @Override
//     public int getOrder() {
//         return Ordered.LOWEST_PRECEDENCE;
//     }
//
//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                     HttpServletResponse response,
//                                     FilterChain filterChain)
//             throws ServletException, IOException {
//
//         long start = System.currentTimeMillis();
//
//         try {
//             filterChain.doFilter(request, response);
//         } finally {
//             long latency = System.currentTimeMillis() - start;
//             Integer statusCode = response.getStatus();
//
//             log.info("[Gateway] {} {} -> {}ms status={}",
//                     request.getMethod(),
//                     request.getRequestURI(),
//                     latency,
//                     statusCode);
//         }
//     }
// }
*/
