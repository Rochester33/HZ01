package com.hz01.frontend.views.alert;

import com.hz01.frontend.client.AlertApiClient;
import com.hz01.frontend.dto.AlertThresholdDto;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "thresholds", layout = MainLayout.class)
@PageTitle("Thresholds")
public class ThresholdView extends VerticalLayout implements LocaleChangeObserver {

    private final AlertApiClient alertApiClient;

    private static final List<String> SENSORS = List.of(
            "temperature", "humidity", "oxygen", "co_level", "battery_level");

    private Select<String> sensorSelect;
    private NumberField warnMin;
    private NumberField warnMax;
    private NumberField critMin;
    private NumberField critMax;
    private FormLayout form;
    private Button saveBtn;

    // Cache loaded thresholds
    private Map<String, AlertThresholdDto> byType = new HashMap<>();

    public ThresholdView(AlertApiClient alertApiClient) {
        this.alertApiClient = alertApiClient;
        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.STRETCH);
        buildUI();
    }

    private void buildUI() {
        removeAll();

        add(new H2(getTranslation("nav.thresholds")));

        // Load current thresholds
        List<AlertThresholdDto> thresholds = alertApiClient.getThresholds();
        byType.clear();
        thresholds.forEach(t -> byType.put(t.sensorType(), t));

        // Sensor dropdown
        sensorSelect = new Select<>();
        sensorSelect.setLabel(getTranslation("threshold.select.sensor"));
        sensorSelect.setItems(SENSORS);
        sensorSelect.setItemLabelGenerator(this::sensorLabel);
        sensorSelect.setWidth("300px");

        // Fields
        warnMin = new NumberField(getTranslation("threshold.warning.level") + " Min");
        warnMax = new NumberField(getTranslation("threshold.warning.level") + " Max");
        critMin = new NumberField(getTranslation("threshold.critical.level") + " Min");
        critMax = new NumberField(getTranslation("threshold.critical.level") + " Max");
        for (NumberField f : new NumberField[]{warnMin, warnMax, critMin, critMax}) {
            f.setClearButtonVisible(true);
            f.setVisible(false);
        }

        form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));
        form.add(warnMin, warnMax, critMin, critMax);

        saveBtn = new Button(getTranslation("threshold.save"), e -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setVisible(false);

        sensorSelect.addValueChangeListener(e -> onSensorSelected(e.getValue()));

        // Default: select first sensor
        sensorSelect.setValue(SENSORS.get(0));

        add(sensorSelect, form, new HorizontalLayout(saveBtn));
    }

    private String sensorLabel(String sensor) {
        String key = sensor.replace("_level", "").replace("co_", "co");
        return getTranslation("sensor." + key);
    }

    private void onSensorSelected(String sensor) {
        if (sensor == null) return;

        AlertThresholdDto t = byType.get(sensor);
        String unit = t != null && t.unit() != null ? " (" + t.unit() + ")" : "";

        warnMin.setLabel(getTranslation("threshold.warning.level") + " Min" + unit);
        warnMax.setLabel(getTranslation("threshold.warning.level") + " Max" + unit);
        critMin.setLabel(getTranslation("threshold.critical.level") + " Min" + unit);
        critMax.setLabel(getTranslation("threshold.critical.level") + " Max" + unit);

        warnMin.setValue(t != null && t.warningMin() != null ? t.warningMin() : null);
        warnMax.setValue(t != null && t.warningMax() != null ? t.warningMax() : null);
        critMin.setValue(t != null && t.criticalMin() != null ? t.criticalMin() : null);
        critMax.setValue(t != null && t.criticalMax() != null ? t.criticalMax() : null);

        for (NumberField f : new NumberField[]{warnMin, warnMax, critMin, critMax}) {
            f.setVisible(true);
        }
        saveBtn.setVisible(true);
    }

    private void save() {
        String sensor = sensorSelect.getValue();
        if (sensor == null) return;
        try {
            AlertThresholdDto dto = new AlertThresholdDto(
                    null, null, sensor,
                    warnMin.getValue(),
                    warnMax.getValue(),
                    critMin.getValue(),
                    critMax.getValue(),
                    null, null);
            alertApiClient.upsertThreshold(dto);
            // Refresh cache
            byType.put(sensor, dto);
            Notification n = Notification.show(getTranslation("threshold.save.success"), 3000,
                    Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification n = Notification.show(getTranslation("common.error.api"), 4000,
                    Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildUI();
    }
}
