package com.brack.memorygame.socket;

import com.brack.memorygame.socket.model.GameMessage;
import com.brack.memorygame.socket.model.GameSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.brack.memorygame.gameplay.CellOwner.*;

public class GameSocketHandler implements WebSocketHandler {

    private final Logger logger = LoggerFactory.getLogger(GameSocketHandler.class);

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var gameSession = GameSession.getSession();
        var player = gameSession.isLobbyOpen() ? PLAYER_A : PLAYER_B;

        Flux<GameMessage> playerInput =
            session.receive()
                .doOnComplete(() -> logger.info("terminated"))
                .map(WebSocketMessage::getPayloadAsText)
                .map(GameMessage::of);

        Flux<GameMessage> playerFeedback =
            gameSession.playerFeedback(player, playerInput);

        return session.send(
            playerFeedback.map(GameMessage::write).map(session::textMessage)
        )
            .doOnCancel(() -> logger.info("session cancelled"))
            .doOnTerminate(() -> logger.info("session terminated"))
            .doOnError(ex -> logger.info("session error: {}", ex.getMessage()));
    }
}
