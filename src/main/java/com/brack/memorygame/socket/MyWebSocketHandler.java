package com.brack.memorygame.socket;

import com.brack.memorygame.gameplay.CellOwner;
import com.brack.memorygame.gameplay.GameSession;
import com.brack.memorygame.socket.model.GameMessage;
import com.brack.memorygame.socket.model.MessageType;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MyWebSocketHandler implements WebSocketHandler {

    private static final String START_MESSAGE =
            new GameMessage("Waiting...", MessageType.INFO).write();

    private static final GameSession gameSession = new GameSession();
    private static final Map<GameSession, List<WebSocketSession>> gameToSocket = new HashMap<>();

    @NotNull
    @Override
    public Mono<Void> handle(WebSocketSession session) {

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
                    gameToSocket.put(new GameSession(), sessions);
                    return java.util.Optional.of(List.of(session.send(Mono.just(session.textMessage(START_MESSAGE)))));
                })
                .map(Flux::concat).orElseThrow();

        Mono<WebSocketMessage> startMessage = Mono.just(session.textMessage(START_MESSAGE));
        Flux<WebSocketMessage> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(GameMessage::of)
                .filter(msg -> msg.getType() == MessageType.CLIENT_CLICK)
                .map(msg -> {
                    int index = Integer.parseInt(msg.getText());
                    gameSession.updateCell(index, CellOwner.PLAYER_A);
                    return new GameMessage(msg.getText(), MessageType.SHOW_FIGURE).write();
                })
                .map(session::textMessage)
                .doOnError(ex -> System.out.println(ex.getMessage()));

        return initialization.then(session.send(Flux.merge(startMessage, input)));
    }
}
