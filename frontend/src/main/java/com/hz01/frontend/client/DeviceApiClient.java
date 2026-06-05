package com.hz01.frontend.client;

import com.hz01.frontend.dto.DeviceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class DeviceApiClient {

    private static final Logger log = LoggerFactory.getLogger(DeviceApiClient.class);
    private final WebClient webClient;

    public DeviceApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<DeviceDto> getAllDevices() {
        try {
            log.info("Fetching devices from backend API...");
            List<DeviceDto> result = webClient.get()
                    .uri("/api/v1/devices")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DeviceDto>>() {})
                    .block();
            log.info("Received {} devices from backend", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch devices from backend: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
