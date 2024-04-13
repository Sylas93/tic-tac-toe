package com.brack.memorygame.socket;

import com.brack.memorygame.gameplay.GameBoard;
import com.brack.memorygame.socket.model.GameMessage;
import com.brack.memorygame.socket.model.GameSession;
import com.brack.memorygame.socket.model.MessageType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.function.Predicate.*;

import static com.brack.memorygame.gameplay.CellOwner.*;

public class GameSocketHandler implements WebSocketHandler {

    private Logger logger = LoggerFactory.getLogger(GameSocketHandler.class);
    private static final String START_MESSAGE =
            new GameMessage("Waiting...", MessageType.INFO).write();

    private static final GameBoard GAME_BOARD = new GameBoard();
    private static final Map<GameBoard, List<WebSocketSession>> gameToSocket = new HashMap<>();
    private static final List<GameSession> gameSessions = new ArrayList<>();

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {

        var gameSession = gameSessions.stream()
                .filter(not(GameSession::getGameStarted))
                .findFirst()
                .map(it -> {
                    it.setGameStarted(true);
                    return it;
                }).orElseGet(() -> {
                    var it = new GameSession();
                    gameSessions.add(it);
                    return it;
                });

        var player = gameSession.getGameStarted() ? PLAYER_B : PLAYER_A;

        Flux<GameMessage> playerFlux =
        session.receive()
            .doOnComplete( () -> logger.info("terminated"))
            .map(WebSocketMessage::getPayloadAsText)
            .map(GameMessage::of)
            .concatMap(it -> gameSession.handleMessage(player, it));

        Flux<GameMessage> opponentFlux = gameSession.getOpponentSink(player).asFlux();

        return session.send(
            playerFlux.mergeWith(opponentFlux).map(GameMessage::write).map(session::textMessage)
        );
        /*
            var initialization = gameToSocket.entrySet().stream().filter(e -> e.getValue().size() == 1)
                .findFirst()
                .map(e -> {
                    e.getValue().add(session);
                    return e.getValue().stream().map(s ->
                        s.send(Mono.just(s.textMessage(
                            new GameMessage("All client joined", MessageType.INFO).write()
                        )))
                    );
                })
                .map(Stream::toList)
                .or(() -> {
                    var sessions = new ArrayList<WebSocketSession>();
                    sessions.add(session);
                    gameToSocket.put(new GameBoard(), sessions);
                    return java.util.Optional.of(List.of(session.send(Mono.just(session.textMessage(START_MESSAGE)))));
                })
                .map(Flux::concat).orElseThrow();



        Flux<WebSocketMessage> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(GameMessage::of)
                .filter(msg -> msg.getType() == MessageType.CLIENT_CLICK)
                .map(msg -> {
                    int index = Integer.parseInt(msg.getText());
                    GAME_BOARD.updateCell(index, PLAYER_A);
                    return new GameMessage(msg.getText(), MessageType.SHOW_FIGURE).write();
                })
                .map(session::textMessage)
                .doOnError(ex -> System.out.println(ex.getMessage()));

        return initialization.then(session.send(input));
        */
    }
}
