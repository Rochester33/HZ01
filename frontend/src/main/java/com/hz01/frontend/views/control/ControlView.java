package com.hz01.frontend.views.control;

import com.hz01.frontend.client.DeviceApiClient;
import com.hz01.frontend.dto.DeviceDto;
import com.hz01.frontend.service.CommandService;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "control", layout = MainLayout.class)
@PageTitle("Control")
public class ControlView extends VerticalLayout implements LocaleChangeObserver {

    private final DeviceApiClient deviceApiClient;
    private final CommandService commandService;

    private final Select<String> deviceSelect = new Select<>();

    public ControlView(DeviceApiClient deviceApiClient, CommandService commandService) {
        this.deviceApiClient = deviceApiClient;
        this.commandService = commandService;
        setSizeFull();
        setPadding(true);
        buildLayout();
    }

    private void buildLayout() {
        removeAll();
        add(new H2(getTranslation("nav.control")));

        List<DeviceDto> devices = deviceApiClient.getAllDevices();
        List<String> deviceIds = devices.stream().map(DeviceDto::deviceId).toList();

        deviceSelect.setLabel(getTranslation("control.select_device"));
        deviceSelect.setItems(deviceIds);
        if (!deviceIds.isEmpty()) deviceSelect.setValue(deviceIds.get(0));
        deviceSelect.setWidthFull();

        TabSheet tabs = new TabSheet();
        tabs.add(new Tab(getTranslation("control.buzzer")), buildTogglePanel("buzzer"));
        tabs.add(new Tab(getTranslation("control.led")), buildLedPanel());
        tabs.setWidthFull();

        add(deviceSelect, tabs);
    }

    /**
     * Build a simple ON/OFF toggle panel for a given device type (buzzer or led).
     */
    private VerticalLayout buildTogglePanel(String type) {
        Button onBtn = new Button(getTranslation("control.action.on"));
        onBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        onBtn.addClickListener(e -> sendCommand(type, "on", 0));

        Button offBtn = new Button(getTranslation("control.action.off"));
        offBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        offBtn.addClickListener(e -> sendCommand(type, "off", 0));

        HorizontalLayout btnRow = new HorizontalLayout(onBtn, offBtn);
        btnRow.setSpacing(true);
        btnRow.setPadding(false);
        btnRow.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout panel = new VerticalLayout(
                new H4(getTranslation("control." + type)),
                btnRow);
        panel.setPadding(true);
        return panel;
    }

    /**
     * Build the LED panel with ON/OFF toggles and an SOS button.
     * SOS triggers a three-short three-long three-short blink pattern on the ESP32 power LED,
     * with the buzzer synchronized.
     */
    private VerticalLayout buildLedPanel() {
        Button onBtn = new Button(getTranslation("control.action.on"));
        onBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        onBtn.addClickListener(e -> sendCommand("led", "on", 0));

        Button offBtn = new Button(getTranslation("control.action.off"));
        offBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        offBtn.addClickListener(e -> sendCommand("led", "off", 0));

        Button sosBtn = new Button(getTranslation("control.action.sos"));
        sosBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        sosBtn.getStyle().set("font-weight", "bold");
        sosBtn.addClickListener(e -> {
            String deviceId = deviceSelect.getValue();
            if (deviceId == null) return;
            // Send SOS to both LED and buzzer so they are synchronized
            boolean ledOk = commandService.sendLed(deviceId, "sos", 0);
            boolean buzzerOk = commandService.sendBuzzer(deviceId, "sos", 0);
            boolean ok = ledOk && buzzerOk;
            showNotification(ok);
        });

        Paragraph sosDesc = new Paragraph(getTranslation("control.sos.description"));
        sosDesc.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout btnRow = new HorizontalLayout(onBtn, offBtn, sosBtn);
        btnRow.setSpacing(true);
        btnRow.setPadding(false);
        btnRow.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout panel = new VerticalLayout(
                new H4(getTranslation("control.led")),
                btnRow,
                sosDesc);
        panel.setPadding(true);
        return panel;
    }

    private void sendCommand(String type, String action, int duration) {
        String deviceId = deviceSelect.getValue();
        if (deviceId == null) return;
        boolean ok = "buzzer".equals(type)
                ? commandService.sendBuzzer(deviceId, action, duration)
                : commandService.sendLed(deviceId, action, duration);
        showNotification(ok);
    }

    private void showNotification(boolean ok) {
        Notification n = Notification.show(
                ok ? getTranslation("control.send.success") : getTranslation("control.send.fail"),
                3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildLayout();
    }
}
