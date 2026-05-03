package com.hz01.frontend.views.control;

import com.hz01.frontend.client.DeviceApiClient;
import com.hz01.frontend.dto.DeviceDto;
import com.hz01.frontend.service.CommandService;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
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
        tabs.add(new Tab(getTranslation("control.buzzer")), buildCommandPanel("buzzer"));
        tabs.add(new Tab(getTranslation("control.led")), buildCommandPanel("led"));
        tabs.setWidthFull();

        add(deviceSelect, tabs);
    }

    private VerticalLayout buildCommandPanel(String type) {
        RadioButtonGroup<String> actionGroup = new RadioButtonGroup<>();
        actionGroup.setLabel(getTranslation("control.action"));
        actionGroup.setItems(
                getTranslation("control.action.on"),
                getTranslation("control.action.off"),
                getTranslation("control.action.blink"));
        actionGroup.setValue(getTranslation("control.action.on"));

        IntegerField durationField = new IntegerField(getTranslation("control.duration"));
        durationField.setValue(5);
        durationField.setMin(0);
        durationField.setMax(3600);
        durationField.setStepButtonsVisible(true);

        Button sendBtn = new Button(getTranslation("control.send"));
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendBtn.addClickListener(e -> {
            String deviceId = deviceSelect.getValue();
            if (deviceId == null) return;

            String actionLabel = actionGroup.getValue();
            String action = labelToAction(actionLabel);
            int duration = durationField.getValue() != null ? durationField.getValue() : 0;

            boolean ok = "buzzer".equals(type)
                    ? commandService.sendBuzzer(deviceId, action, duration)
                    : commandService.sendLed(deviceId, action, duration);

            Notification n = Notification.show(
                    ok ? getTranslation("control.send.success") : getTranslation("control.send.fail"),
                    3000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
        });

        VerticalLayout panel = new VerticalLayout(
                new H4(getTranslation("control." + type)),
                actionGroup,
                buildSendRow(durationField, sendBtn));
        panel.setPadding(true);
        return panel;
    }

    private HorizontalLayout buildSendRow(IntegerField durationField, Button sendBtn) {
        HorizontalLayout row = new HorizontalLayout(durationField, sendBtn);
        row.setAlignItems(FlexComponent.Alignment.END);
        row.setPadding(false);
        return row;
    }

    private String labelToAction(String label) {
        if (label == null) return "on";
        String lower = label.toLowerCase();
        if (lower.contains("off") || lower.contains("关") || lower.contains("mati")) return "off";
        if (lower.contains("blink") || lower.contains("闪") || lower.contains("berkelip")) return "blink";
        return "on";
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildLayout();
    }
}
