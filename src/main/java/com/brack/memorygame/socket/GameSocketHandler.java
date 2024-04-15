package com.brack.memorygame.socket;

import com.brack.memorygame.socket.model.GameMessage;
import com.brack.memorygame.socket.model.GameSession;
import kotlin.Pair;
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
            gameSession.serverFeedback(playerInput.map(it -> new Pair<>(player, it)));

        Flux<GameMessage> serverInput = gameSession.serverInput(player)
            .doOnCancel(() -> logger.info("server flux cancelled"))
            .doOnComplete(() -> logger.info("server flux completed"));

        return session.send(
            playerFeedback.mergeWith(serverInput)
                .takeUntil(GameMessage::isLast)
                .map(GameMessage::write).map(session::textMessage)
        );
    }
}
