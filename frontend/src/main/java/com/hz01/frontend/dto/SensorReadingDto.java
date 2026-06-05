package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SensorReadingDto(
        Long id,
        @JsonProperty("device_id") String deviceId,
        Double temperature,
        Double humidity,
        Double oxygen,
        @JsonProperty("co_level") Double coLevel,
        @JsonProperty("methane_level") Double methaneLevel,
        @JsonProperty("battery_level") Double batteryLevel,
        @JsonProperty("recorded_at") OffsetDateTime recordedAt,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {}
