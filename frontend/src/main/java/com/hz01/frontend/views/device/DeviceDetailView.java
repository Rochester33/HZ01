package com.hz01.frontend.views.device;

import com.hz01.frontend.client.SensorApiClient;
import com.hz01.frontend.dto.SensorReadingDto;
import com.hz01.frontend.service.CommandService;
import com.hz01.frontend.service.RealtimeEventBus;
import com.hz01.frontend.views.MainLayout;
import com.hz01.frontend.views.components.SensorCard;
import com.hz01.frontend.views.components.StatusBadge;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.*;
import reactor.core.Disposable;

@Route(value = "devices/:deviceId", layout = MainLayout.class)
@PageTitle("Device Detail")
public class DeviceDetailView extends VerticalLayout
        implements HasUrlParameter<String>, LocaleChangeObserver {

    private final SensorApiClient sensorApiClient;
    private final CommandService commandService;
    private final RealtimeEventBus eventBus;

    private String deviceId;
    private final StatusBadge statusBadge = new StatusBadge("offline");
    private final H2 deviceTitle = new H2();

    private final SensorCard tempCard = new SensorCard("", "°C");
    private final SensorCard humCard = new SensorCard("", "%");
    private final SensorCard coCard = new SensorCard("", "ppm");
    private final SensorCard methaneCard = new SensorCard("", "ppm");
    private final SensorCard batCard = new SensorCard("", "%");

    private Disposable subscription;

    public DeviceDetailView(SensorApiClient sensorApiClient,
                            CommandService commandService,
                            RealtimeEventBus eventBus) {
        this.sensorApiClient = sensorApiClient;
        this.commandService = commandService;
        this.eventBus = eventBus;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.deviceId = parameter;
        buildLayout();
        loadLatest();
    }

    private void buildLayout() {
        removeAll();

        HorizontalLayout header = new HorizontalLayout(deviceTitle, statusBadge);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        updateCardTitles();

        tempCard.setThresholds(-10.0, 40.0, -20.0, 45.0);
        humCard.setThresholds(20.0, 80.0, 10.0, 95.0);
        coCard.setThresholds(null, 2000.0, null, 3000.0);
        methaneCard.setThresholds(null, 2000.0, null, 3000.0);
        batCard.setThresholds(20.0, null, 10.0, null);

        HorizontalLayout cards = new HorizontalLayout(tempCard, humCard, coCard, methaneCard, batCard);
        cards.setWidthFull();
        cards.setFlexGrow(1, tempCard, humCard, coCard, methaneCard, batCard);

        Button buzzerOn = new Button(getTranslation("control.buzzer") + " ON",
                e -> sendCmd("buzzer", "on"));
        Button buzzerOff = new Button(getTranslation("control.buzzer") + " OFF",
                e -> sendCmd("buzzer", "off"));
        Button ledBlink = new Button(getTranslation("control.led") + " BLINK",
                e -> sendCmd("led", "blink"));
        Button ledOff = new Button(getTranslation("control.led") + " OFF",
                e -> sendCmd("led", "off"));

        buzzerOn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        ledBlink.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout controls = new HorizontalLayout(buzzerOn, buzzerOff, ledBlink, ledOff);

        // SOS toggle row — full red with warning indicator
        com.vaadin.flow.component.html.Span sosWarning = new com.vaadin.flow.component.html.Span(
                "⚠ " + getTranslation("control.sos.label"));
        sosWarning.getStyle()
                .set("color", "white")
                .set("font-weight", "bold")
                .set("font-size", "1em")
                .set("align-self", "center");

        com.vaadin.flow.component.checkbox.Checkbox sosToggle = new com.vaadin.flow.component.checkbox.Checkbox(
                getTranslation("control.sos.toggle"));
        sosToggle.getStyle().set("color", "white").set("font-weight", "bold");
        sosToggle.addValueChangeListener(e -> {
            boolean active = e.getValue();
            boolean ok;
            if (active) {
                ok = commandService.sendSos(deviceId);
            } else {
                // Turn off SOS: stop both LED and buzzer
                ok = commandService.sendLed(deviceId, "off", 0)
                        & commandService.sendBuzzer(deviceId, "off", 0);
            }

            Span text = new Span(ok ? getTranslation("control.send.success") : getTranslation("control.send.fail"));
            text.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("max-width", "300px")
                .set("word-wrap", "break-word");

            Notification n = new Notification(text);
            n.addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
            n.setPosition(Notification.Position.BOTTOM_END);
            n.setDuration(3000);
            n.open();

            if (!ok) sosToggle.setValue(!active); // revert on failure
        });

        HorizontalLayout sosRow = new HorizontalLayout(sosWarning, sosToggle);
        sosRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        sosRow.getStyle()
                .set("background-color", "#c00000")
                .set("padding", "10px 16px")
                .set("border-radius", "6px")
                .set("gap", "16px");

        add(header, cards, controls, sosRow);
    }

    private void updateCardTitles() {
        deviceTitle.setText(deviceId);
    }

    private void loadLatest() {
        sensorApiClient.getHistory(deviceId, 1).stream().findFirst().ifPresent(this::applyReading);
    }

    private void applyReading(SensorReadingDto r) {
        tempCard.update(r.temperature());
        humCard.update(r.humidity());
        coCard.update(r.coLevel());
        methaneCard.update(r.methaneLevel());
        batCard.update(r.batteryLevel());
    }

    private void sendCmd(String type, String action) {
        boolean ok = "buzzer".equals(type)
                ? commandService.sendBuzzer(deviceId, action, 5)
                : commandService.sendLed(deviceId, action, 5);

        Span text = new Span(ok ? getTranslation("control.send.success") : getTranslation("control.send.fail"));
        text.getStyle()
            .set("padding", "var(--lumo-space-m)")
            .set("max-width", "300px")
            .set("word-wrap", "break-word");

        Notification n = new Notification(text);
        n.addThemeVariants(ok ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
        n.setPosition(Notification.Position.BOTTOM_END);
        n.setDuration(3000);
        n.open();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        UI ui = event.getUI();
        subscription = eventBus.subscribeReadings()
                .filter(msg -> deviceId != null && deviceId.equals(msg.deviceId()))
                .subscribe(msg -> ui.access(() -> applyReading(new SensorReadingDto(
                        null, msg.deviceId(),
                        msg.temperature(), msg.humidity(), msg.oxygen(),
                        msg.coLevel(), msg.methaneLevel(), msg.batteryLevel(), null, null))));
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (subscription != null) subscription.dispose();
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildLayout();
    }
}
