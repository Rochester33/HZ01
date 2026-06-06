package com.hz01.frontend.views.alert;

import com.hz01.frontend.client.AlertApiClient;
import com.hz01.frontend.dto.AlertEventDto;
import com.hz01.frontend.service.RealtimeEventBus;
import com.hz01.frontend.views.MainLayout;
import com.hz01.frontend.views.components.CustomNotification;
import com.hz01.frontend.views.components.StatusBadge;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import reactor.core.Disposable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "alerts", layout = MainLayout.class)
@PageTitle("Alerts")
public class AlertView extends VerticalLayout implements LocaleChangeObserver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertApiClient alertApiClient;
    private final RealtimeEventBus eventBus;

    private final Grid<AlertEventDto> grid = new Grid<>(AlertEventDto.class, false);
    private final List<AlertEventDto> events = new ArrayList<>();
    private Disposable subscription;

    public AlertView(AlertApiClient alertApiClient, RealtimeEventBus eventBus) {
        this.alertApiClient = alertApiClient;
        this.eventBus = eventBus;
        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.STRETCH);

        H2 title = new H2(getTranslation("nav.alerts"));
        Button refresh = new Button(getTranslation("common.refresh"), e -> loadData());
        HorizontalLayout toolbar = new HorizontalLayout(title, refresh);
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.expand(title);

        configureGrid();
        add(toolbar, grid);
        loadData();
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSizeFull();

        grid.addColumn(e -> e.triggeredAt() != null ? e.triggeredAt().format(FMT) : "--")
                .setHeader(getTranslation("alert.time")).setAutoWidth(true);
        grid.addColumn(AlertEventDto::deviceId).setHeader(getTranslation("sensor.device")).setAutoWidth(true);
        grid.addColumn(AlertEventDto::sensorType).setHeader(getTranslation("alert.sensor")).setAutoWidth(true);
        grid.addColumn(e -> e.value() != null ? String.format("%.2f", e.value()) : "--")
                .setHeader(getTranslation("alert.value")).setAutoWidth(true);
        grid.addColumn(e -> e.threshold() != null ? String.format("%.2f", e.threshold()) : "--")
                .setHeader(getTranslation("alert.threshold")).setAutoWidth(true);
        grid.addComponentColumn(e -> new StatusBadge(e.level()))
                .setHeader(getTranslation("alert.level")).setAutoWidth(true);
        grid.addComponentColumn(e -> {
            if (Boolean.TRUE.equals(e.acknowledged())) {
                return new com.vaadin.flow.component.html.Span(getTranslation("alert.acknowledged"));
            }
            Button btn = new Button(getTranslation("alert.acknowledge.btn"));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            btn.addClickListener(ev -> {
                alertApiClient.acknowledgeEvent(e.id());

                CustomNotification.showSuccess(getTranslation("alert.acknowledge.success"));

                loadData();
            });
            return btn;
        }).setHeader(getTranslation("alert.action")).setAutoWidth(true);

        grid.setClassNameGenerator(e -> "critical".equals(e.level()) ? "row-critical" : "row-warning");
    }

    private void loadData() {
        events.clear();
        events.addAll(alertApiClient.getEvents(null));
        grid.setItems(new ArrayList<>(events));
    }

    @Override
    protected void onAttach(AttachEvent event) {
        UI ui = event.getUI();
        subscription = eventBus.subscribeAlerts().subscribe(msg -> {
            ui.access(() -> {
                String message = msg.deviceId() + " — " + msg.sensorType() + ": " + msg.value();
                Span text = new Span(message);
                text.getStyle()
                    .set("display", "inline-block")
                    .set("padding", "var(--lumo-space-m)")
                    .set("max-width", "400px")
                    .set("word-wrap", "break-word")
                    .set("white-space", "normal");

                Notification n = new Notification(text);
                if ("critical".equals(msg.level())) {
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    n.addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
                n.setPosition(Notification.Position.TOP_END);
                n.setDuration(5000);
                n.open();

                loadData();
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (subscription != null) subscription.dispose();
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        grid.removeAllColumns();
        configureGrid();
        grid.setItems(new ArrayList<>(events));
    }
}
