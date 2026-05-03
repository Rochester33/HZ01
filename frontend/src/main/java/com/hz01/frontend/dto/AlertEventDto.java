package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertEventDto(
        Long id,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("sensor_type") String sensorType,
        String level,
        Double value,
        Double threshold,
        String message,
        Boolean acknowledged,
        @JsonProperty("triggered_at") OffsetDateTime triggeredAt,
        @JsonProperty("acknowledged_at") OffsetDateTime acknowledgedAt
) {}
