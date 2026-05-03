package com.hz01.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommandRequestDto(
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("command_type") String commandType,
        String action,
        int duration
) {}
