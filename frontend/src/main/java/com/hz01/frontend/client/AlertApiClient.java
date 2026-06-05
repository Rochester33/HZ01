package com.hz01.frontend.client;

import com.hz01.frontend.dto.AlertEventDto;
import com.hz01.frontend.dto.AlertThresholdDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class AlertApiClient {

    private final WebClient webClient;

    public AlertApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<AlertThresholdDto> getThresholds() {
        try {
            List<AlertThresholdDto> result = webClient.get()
                    .uri("/api/v1/alerts/thresholds")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<AlertThresholdDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<AlertThresholdDto> getDeviceThresholds(String deviceId) {
        try {
            List<AlertThresholdDto> result = webClient.get()
                    .uri("/api/v1/alerts/thresholds/device/" + deviceId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<AlertThresholdDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void upsertThreshold(AlertThresholdDto dto) {
        try {
            webClient.put()
                    .uri("/api/v1/alerts/thresholds")
                    .bodyValue(dto)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ignored) {}
    }

    public List<AlertEventDto> getEvents(Boolean acknowledged) {
        try {
            List<AlertEventDto> result = webClient.get()
                    .uri(u -> {
                        var builder = u.path("/api/v1/alerts/events");
                        if (acknowledged != null) {
                            builder = builder.queryParam("acknowledged", acknowledged);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<AlertEventDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void acknowledgeEvent(Long eventId) {
        try {
            webClient.patch()
                    .uri("/api/v1/alerts/events/" + eventId + "/acknowledge")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ignored) {}
    }
}
