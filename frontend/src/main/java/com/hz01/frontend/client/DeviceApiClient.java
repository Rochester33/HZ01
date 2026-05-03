package com.hz01.frontend.client;

import com.hz01.frontend.dto.DeviceDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class DeviceApiClient {

    private final WebClient webClient;

    public DeviceApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<DeviceDto> getAllDevices() {
        try {
            List<DeviceDto> result = webClient.get()
                    .uri("/api/v1/devices")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DeviceDto>>() {})
                    .block();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
