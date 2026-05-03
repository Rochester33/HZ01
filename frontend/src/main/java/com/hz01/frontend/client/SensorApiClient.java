package com.hz01.frontend.client;

import com.hz01.frontend.dto.SensorReadingDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class SensorApiClient {

    private final WebClient webClient;

    public SensorApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<SensorReadingDto> getLatestReadings() {
        try {
            List<SensorReadingDto> result = webClient.get()
                    .uri("/api/v1/sensors/readings/latest")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<SensorReadingDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<SensorReadingDto> getHistory(String deviceId, int limit) {
        try {
            List<SensorReadingDto> result = webClient.get()
                    .uri(u -> u.path("/api/v1/sensors/readings")
                            .queryParam("device_id", deviceId)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<SensorReadingDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void simulate(String deviceId) {
        try {
            webClient.post()
                    .uri("/api/v1/simulate/" + deviceId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ignored) {}
    }
}
