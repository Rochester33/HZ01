package com.hz01.frontend.views.control;

import com.hz01.frontend.client.DeviceApiClient;
import com.hz01.frontend.dto.DeviceDto;
import com.hz01.frontend.service.CommandService;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
        deviceSelect.setEmptySelectionAllowed(false);
        if (!deviceIds.isEmpty()) {
            deviceSelect.setValue(deviceIds.get(0));
        }
        deviceSelect.setWidthFull();

        TabSheet tabs = new TabSheet();
        tabs.add(new Tab(getTranslation("control.buzzer")), buildTogglePanel("buzzer"));
        tabs.add(new Tab(getTranslation("control.led")), buildLedPanel());
        tabs.add(new Tab(getTranslation("control.emergency")), buildEmergencyPanel());
        tabs.setWidthFull();

        add(deviceSelect, tabs);
    }

    /**
     * Build a simple ON/OFF/AUTO toggle panel for a given device type (buzzer or led).
     */
    private VerticalLayout buildTogglePanel(String type) {
        Button onBtn = new Button(getTranslation("control.action.on"));
        onBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        onBtn.addClickListener(e -> sendCommand(type, "on", 0));

        Button offBtn = new Button(getTranslation("control.action.off"));
        offBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        offBtn.addClickListener(e -> sendCommand(type, "off", 0));

        Button autoBtn = new Button(getTranslation("control.action.blink"));
        autoBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        autoBtn.addClickListener(e -> sendCommand(type, "blink", 0));

        HorizontalLayout btnRow = new HorizontalLayout(onBtn, offBtn, autoBtn);
        btnRow.setSpacing(true);
        btnRow.setPadding(false);
        btnRow.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout panel = new VerticalLayout(
                new H4(getTranslation("control." + type)),
                btnRow);
        panel.setPadding(true);
        return panel;
    }

    private VerticalLayout buildLedPanel() {
        return buildTogglePanel("led");
    }

    /**
     * Emergency panel — contains the SOS button which triggers synchronized LED + buzzer SOS pattern.
     */
    private VerticalLayout buildEmergencyPanel() {
        Span sosLabel = new Span("⚠ " + getTranslation("control.sos.label"));
        sosLabel.getStyle()
                .set("color", "white")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)");

        Button sosBtn = new Button(getTranslation("control.action.sos"));
        sosBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        sosBtn.getStyle().set("font-weight", "bold").set("margin-left", "auto");
        sosBtn.addClickListener(e -> {
            String deviceId = deviceSelect.getValue();
            if (deviceId == null) return;
            boolean ledOk = commandService.sendLed(deviceId, "sos", 0);
            boolean buzzerOk = commandService.sendBuzzer(deviceId, "sos", 0);
            showNotification(ledOk && buzzerOk);
        });

        HorizontalLayout sosRow = new HorizontalLayout(sosLabel, sosBtn);
        sosRow.setWidthFull();
        sosRow.setAlignItems(FlexComponent.Alignment.CENTER);
        sosRow.getStyle()
                .set("background", "#c00000")
                .set("border-radius", "8px")
                .set("padding", "12px 16px");

        Paragraph sosDesc = new Paragraph(getTranslation("control.sos.description"));
        sosDesc.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        VerticalLayout panel = new VerticalLayout(sosRow, sosDesc);
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
        String message = ok ? getTranslation("control.send.success") : getTranslation("control.send.fail");

        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildLayout();
    }
}
