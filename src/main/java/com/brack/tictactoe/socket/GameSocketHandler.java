package com.brack.tictactoe.socket;

import com.brack.tictactoe.socket.model.GameMessage;
import com.brack.tictactoe.socket.model.GameSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GameSocketHandler implements WebSocketHandler {

    private final Logger logger = LoggerFactory.getLogger(GameSocketHandler.class);

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<GameMessage> playerInput =
            session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(GameMessage::of);

        Flux<GameMessage> feedback = GameSession.feedback(playerInput);

        return session.send(
            feedback.map(GameMessage::write).map(session::textMessage)
        ).doOnError(e -> logger.error("Socket error: {}", e.getMessage()));
    }
}
