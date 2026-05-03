package com.hz01.frontend.client;

import com.hz01.frontend.dto.CommandRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CommandApiClient {

    private final WebClient webClient;

    public CommandApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public boolean sendCommand(CommandRequestDto dto) {
        try {
            webClient.post()
                    .uri("/api/v1/commands/")
                    .bodyValue(dto)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
