package com.hz01.frontend.service;

import com.hz01.frontend.client.CommandApiClient;
import com.hz01.frontend.dto.CommandRequestDto;
import org.springframework.stereotype.Service;

@Service
public class CommandService {

    private final CommandApiClient commandApiClient;

    public CommandService(CommandApiClient commandApiClient) {
        this.commandApiClient = commandApiClient;
    }

    public boolean sendBuzzer(String deviceId, String action, int duration) {
        return commandApiClient.sendCommand(new CommandRequestDto(deviceId, "buzzer", action, duration));
    }

    public boolean sendLed(String deviceId, String action, int duration) {
        return commandApiClient.sendCommand(new CommandRequestDto(deviceId, "led", action, duration));
    }
}
