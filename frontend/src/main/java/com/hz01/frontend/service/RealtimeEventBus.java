package com.hz01.frontend.service;

import com.hz01.frontend.dto.WsMessageDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class RealtimeEventBus {

    private final Sinks.Many<WsMessageDto> readingSink =
            Sinks.many().multicast().onBackpressureBuffer(256, false);

    private final Sinks.Many<WsMessageDto> alertSink =
            Sinks.many().multicast().onBackpressureBuffer(256, false);

    public void publishReading(WsMessageDto msg) {
        readingSink.tryEmitNext(msg);
    }

    public void publishAlert(WsMessageDto msg) {
        alertSink.tryEmitNext(msg);
    }

    public Flux<WsMessageDto> subscribeReadings() {
        return readingSink.asFlux();
    }

    public Flux<WsMessageDto> subscribeAlerts() {
        return alertSink.asFlux();
    }
}
