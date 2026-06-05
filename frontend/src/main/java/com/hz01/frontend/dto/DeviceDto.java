package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceDto(
        Integer id,
        @JsonProperty("device_id") String deviceId,
        String name,
        String location,
        String status,
        @JsonProperty("last_seen") LocalDateTime lastSeen,
        @JsonProperty("created_at") LocalDateTime createdAt
) {}
