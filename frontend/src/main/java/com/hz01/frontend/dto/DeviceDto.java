package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceDto(
        Integer id,
        @JsonProperty("device_id") String deviceId,
        String name,
        String location,
        String status,
        @JsonProperty("last_seen") OffsetDateTime lastSeen,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {}
