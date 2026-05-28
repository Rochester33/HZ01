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

    /**
     * Send SOS signal: three short, three long, three short flashes on LED and buzzer simultaneously.
     * Duration 0 means the ESP32 handles the full SOS sequence internally.
     */
    public boolean sendSos(String deviceId) {
        boolean ledOk = commandApiClient.sendCommand(new CommandRequestDto(deviceId, "led", "sos", 0));
        boolean buzzerOk = commandApiClient.sendCommand(new CommandRequestDto(deviceId, "buzzer", "sos", 0));
        return ledOk && buzzerOk;
    }
}
