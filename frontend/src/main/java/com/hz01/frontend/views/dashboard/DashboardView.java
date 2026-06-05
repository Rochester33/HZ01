package com.hz01.frontend.views.dashboard;

import com.hz01.frontend.client.SensorApiClient;
import com.hz01.frontend.dto.SensorReadingDto;
import com.hz01.frontend.service.RealtimeEventBus;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Route(value = "", layout = MainLayout.class)
@PageTitle("HZ-01 Dashboard")
public class DashboardView extends VerticalLayout implements LocaleChangeObserver {

    private final SensorApiClient sensorApiClient;
    private final RealtimeEventBus eventBus;

    private final Grid<SensorReadingDto> grid = new Grid<>(SensorReadingDto.class, false);
    /** Keyed by deviceId so WebSocket updates replace rather than duplicate/remove entries */
    private final Map<String, SensorReadingDto> readingsMap = new LinkedHashMap<>();
    private final H2 title = new H2();
    private Disposable subscription;

    public DashboardView(SensorApiClient sensorApiClient, RealtimeEventBus eventBus) {
        this.sensorApiClient = sensorApiClient;
        this.eventBus = eventBus;

        setSizeFull();
        setPadding(true);

        title.setText(getTranslation("nav.dashboard"));
        configureGrid();
        add(title, grid);

        loadData();
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSizeFull();

        grid.addColumn(SensorReadingDto::deviceId).setHeader(getTranslation("sensor.device")).setAutoWidth(true);
        grid.addColumn(r -> formatVal(r.temperature(), "°C")).setHeader(getTranslation("sensor.temperature")).setAutoWidth(true);
        grid.addColumn(r -> formatVal(r.humidity(), "%")).setHeader(getTranslation("sensor.humidity")).setAutoWidth(true);
        grid.addColumn(r -> formatVal(r.coLevel(), " ppm")).setHeader(getTranslation("sensor.co")).setAutoWidth(true);
        grid.addColumn(r -> formatVal(r.methaneLevel(), " ppm")).setHeader(getTranslation("sensor.methane")).setAutoWidth(true);
        grid.addColumn(r -> formatVal(r.batteryLevel(), "%")).setHeader(getTranslation("sensor.battery")).setAutoWidth(true);
        grid.addComponentColumn(r -> statusBadge(r)).setHeader(getTranslation("sensor.status")).setAutoWidth(true);

        grid.setClassNameGenerator(r -> "row-" + computeLevel(r));
    }

    private String formatVal(Double val, String unit) {
        return val == null ? "--" : String.format("%.1f%s", val, unit);
    }

    private Span statusBadge(SensorReadingDto r) {
        String level = computeLevel(r);
        Span badge = new Span(getTranslation("status." + level));
        badge.addClassName("badge-" + level);
        return badge;
    }

    private String computeLevel(SensorReadingDto r) {
        if (r.coLevel() != null && r.coLevel() > 3000) return "critical";
        if (r.methaneLevel() != null && r.methaneLevel() > 3000) return "critical";
        if (r.temperature() != null && r.temperature() > 45) return "critical";
        if (r.humidity() != null && r.humidity() > 95) return "critical";
        if (r.coLevel() != null && r.coLevel() > 2000) return "warning";
        if (r.methaneLevel() != null && r.methaneLevel() > 2000) return "warning";
        if (r.temperature() != null && r.temperature() > 40) return "warning";
        if (r.humidity() != null && r.humidity() > 80) return "warning";
        if (r.batteryLevel() != null && r.batteryLevel() < 10) return "critical";
        if (r.batteryLevel() != null && r.batteryLevel() < 20) return "warning";
        return "normal";
    }

    private void loadData() {
        readingsMap.clear();
        sensorApiClient.getLatestReadings().forEach(r -> readingsMap.put(r.deviceId(), r));
        grid.setItems(new ArrayList<>(readingsMap.values()));
    }

    @Override
    protected void onAttach(AttachEvent event) {
        UI ui = event.getUI();
        subscription = eventBus.subscribeReadings().subscribe(msg -> {
            ui.access(() -> {
                // Replace the entry for this device — never removes other devices
                readingsMap.put(msg.deviceId(), new SensorReadingDto(
                        null, msg.deviceId(),
                        msg.temperature(), msg.humidity(), msg.oxygen(),
                        msg.coLevel(), msg.methaneLevel(), msg.batteryLevel(), null, null));
                grid.setItems(new ArrayList<>(readingsMap.values()));
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (subscription != null) subscription.dispose();
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        title.setText(getTranslation("nav.dashboard"));
        grid.removeAllColumns();
        configureGrid();
        grid.setItems(new ArrayList<>(readingsMap.values()));
    }
}
