package com.hz01.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hz01.frontend.dto.WsMessageDto;
import com.hz01.frontend.service.RealtimeEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Component
public class WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private final RealtimeEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final String backendUrl;
    private Disposable connection;

    public WebSocketClient(
            RealtimeEventBus eventBus,
            ObjectMapper objectMapper,
            @Value("${hz01.backend.url}") String backendUrl) {
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.backendUrl = backendUrl;
    }

    @PostConstruct
    public void connect() {
        String wsUrl = backendUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws";
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        connection = client.execute(
                URI.create(wsUrl),
                session -> session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(this::handleMessage)
                        .then()
        )
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry(sig -> log.warn("WebSocket disconnected, retrying... attempt {}", sig.totalRetries() + 1)))
        .subscribe(
                null,
                err -> log.error("WebSocket fatal error", err)
        );
    }

    private void handleMessage(String raw) {
        try {
            WsMessageDto msg = objectMapper.readValue(raw, WsMessageDto.class);
            if ("reading".equals(msg.type())) {
                eventBus.publishReading(msg);
            } else if ("alert".equals(msg.type())) {
                eventBus.publishAlert(msg);
            }
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message: {}", raw);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
    }
}
