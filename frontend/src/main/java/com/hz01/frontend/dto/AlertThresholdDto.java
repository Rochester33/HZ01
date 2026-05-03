package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertThresholdDto(
        Integer id,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("sensor_type") String sensorType,
        @JsonProperty("warning_min") Double warningMin,
        @JsonProperty("warning_max") Double warningMax,
        @JsonProperty("critical_min") Double criticalMin,
        @JsonProperty("critical_max") Double criticalMax,
        String unit,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
