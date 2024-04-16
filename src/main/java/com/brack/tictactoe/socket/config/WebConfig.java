package com.brack.tictactoe.socket.config;

import com.brack.tictactoe.socket.GameSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
class WebConfig implements WebFluxConfigurer {

    private static final MediaType APPLICATION_JS = new MediaType("application", "javascript");
    private static final String SOCKET_PLACEHOLDER_JS = "SOCKET_HOST";
    private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(5, TimeUnit.MINUTES);

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/socket", new GameSocketHandler());
        int order = -1; // before annotated controllers

        return new SimpleUrlHandlerMapping(map, order);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CACHE_CONTROL);
    }

    @Bean
    public RouterFunction<ServerResponse> indexRouter(
        @Value("classpath:/static/app.js") final Resource appJS,
        @Value("${socket.host}") final String socketHost
    ) throws IOException {
        var contextAwareAppJS = appJS
            .getContentAsString(Charset.defaultCharset())
            .replace(SOCKET_PLACEHOLDER_JS, socketHost);

        return route(
            GET("/app.js"),
            request -> ok()
                .contentType(APPLICATION_JS)
                .cacheControl(CACHE_CONTROL)
                .bodyValue(contextAwareAppJS)
        );
    }
}
