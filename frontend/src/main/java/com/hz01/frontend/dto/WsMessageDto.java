package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsMessageDto(
        String type,
        @JsonProperty("device_id") String deviceId,
        Double temperature,
        Double humidity,
        Double oxygen,
        @JsonProperty("co_level") Double coLevel,
        @JsonProperty("methane_level") Double methaneLevel,
        @JsonProperty("battery_level") Double batteryLevel,
        @JsonProperty("recorded_at") String recordedAt,
        @JsonProperty("sensor_type") String sensorType,
        String level,
        Double value,
        Double threshold,
        String message,
        @JsonProperty("triggered_at") String triggeredAt
) {}
