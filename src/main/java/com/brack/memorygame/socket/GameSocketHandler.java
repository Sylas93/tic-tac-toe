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

    private Logger logger = LoggerFactory.getLogger(GameSocketHandler.class);

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {

        var gameSession = GameSession.getSession();

        var player = gameSession.isGameOpen() ? PLAYER_A : PLAYER_B;

        Flux<GameMessage> playerFlux =
            session.receive()
                .doOnComplete( () -> logger.info("terminated"))
                .map(WebSocketMessage::getPayloadAsText)
                .map(GameMessage::of)
                .concatMap(it -> gameSession.handleMessage(player, it));

        Flux<GameMessage> serverFlux = gameSession.getSink(player).asFlux()
            .doOnCancel(() -> logger.info("server flux cancelled"))
            .doOnComplete(() -> logger.info("server flux completed"));

        return session.send(
            playerFlux.mergeWith(serverFlux)
                .takeUntil(GameMessage::isLast)
                .map(GameMessage::write).map(session::textMessage)
        );
    }
}
