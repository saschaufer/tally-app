package de.saschaufer.tallyapp.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCFilter implements WebFilter {

    public static final String KEY_MDC = "mdc";

    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getEncoder().withoutPadding();

    @NonNull
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {

        final byte[] buffer = new byte[10];
        random.nextBytes(buffer);
        final String requestId = encoder.encodeToString(buffer);

        final String userId = exchange.getRequest().getHeaders().getFirst("X-UserId");

        MDC.put("requestId", requestId);
        MDC.put("userId", userId);

        return chain.filter(exchange)
                .contextWrite(Context.of(KEY_MDC, MDC.getCopyOfContextMap()));
    }
}
