package com.brack.tictactoe.socket.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebFilterImpl implements WebFilter {

    private final Logger logger = LoggerFactory.getLogger(WebFilterImpl.class);

    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
                             WebFilterChain webFilterChain) {

        var path = serverWebExchange.getRequest().getPath();
        var method = serverWebExchange.getRequest().getMethod();
        logger.debug("Incoming request {} {}", method, path);

        return webFilterChain.filter(serverWebExchange);
    }
}
