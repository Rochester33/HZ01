package com.hz01.frontend.views.alert;

import com.hz01.frontend.client.AlertApiClient;
import com.hz01.frontend.client.DeviceApiClient;
import com.hz01.frontend.dto.AlertThresholdDto;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.html.Span;
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
    private final DeviceApiClient deviceApiClient;

    private static final List<String> SENSORS = List.of(
            "temperature", "humidity", "co_level", "methane_level");

    private Select<String> deviceSelect;
    private Select<String> sensorSelect;
    private NumberField warnMin;
    private NumberField warnMax;
    private NumberField critMin;
    private NumberField critMax;
    private FormLayout form;
    private Button saveBtn;
    private Span hintLabel;

    // Cache loaded thresholds by device
    private Map<String, Map<String, AlertThresholdDto>> thresholdsByDevice = new HashMap<>();

    public ThresholdView(AlertApiClient alertApiClient, DeviceApiClient deviceApiClient) {
        this.alertApiClient = alertApiClient;
        this.deviceApiClient = deviceApiClient;
        setSizeFull();
        setPadding(true);
        setAlignItems(Alignment.STRETCH);
        buildUI();
    }

    private void buildUI() {
        removeAll();

        add(new H2(getTranslation("nav.thresholds")));

        // Device selector
        List<String> deviceIds = deviceApiClient.getAllDevices().stream()
                .map(d -> d.deviceId())
                .toList();

        deviceSelect = new Select<>();
        deviceSelect.setLabel(getTranslation("control.select_device"));
        deviceSelect.setItems(deviceIds);
        deviceSelect.setItemLabelGenerator(id -> id);
        deviceSelect.setWidth("300px");
        deviceSelect.setEmptySelectionAllowed(false);

        if (!deviceIds.isEmpty()) {
            deviceSelect.setValue(deviceIds.get(0));
        }

        // Load thresholds for all devices
        thresholdsByDevice.clear();
        for (String deviceId : deviceIds) {
            Map<String, AlertThresholdDto> deviceThresholds = new HashMap<>();
            List<AlertThresholdDto> thresholds = alertApiClient.getDeviceThresholds(deviceId);
            thresholds.forEach(t -> deviceThresholds.put(t.sensorType(), t));
            thresholdsByDevice.put(deviceId, deviceThresholds);
        }

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

        // Validate: only non-negative integers allowed; empty = keep current; otherwise disable save
        for (NumberField f : new NumberField[]{warnMin, warnMax, critMin, critMax}) {
            f.addValueChangeListener(e -> validateFields());
        }

        deviceSelect.addValueChangeListener(e -> onDeviceChanged());
        sensorSelect.addValueChangeListener(e -> onSensorSelected(e.getValue()));

        // Default: select first sensor if device is selected
        if (!deviceIds.isEmpty()) {
            sensorSelect.setValue(SENSORS.get(0));
        }

        // Hint text above save button
        Span hint = new Span(getTranslation("threshold.hint"));
        hint.getStyle().set("color", "white").set("font-size", "0.85em");

        add(deviceSelect, sensorSelect, form, hint, new HorizontalLayout(saveBtn));
    }

    private String sensorLabel(String sensor) {
        String key = sensor.replace("_level", "").replace("co_", "co");
        return getTranslation("sensor." + key);
    }

    private void onDeviceChanged() {
        String sensor = sensorSelect.getValue();
        if (sensor != null) {
            onSensorSelected(sensor);
        }
    }

    private void onSensorSelected(String sensor) {
        if (sensor == null) return;

        String deviceId = deviceSelect.getValue();
        if (deviceId == null) return;

        Map<String, AlertThresholdDto> deviceThresholds = thresholdsByDevice.get(deviceId);
        AlertThresholdDto t = deviceThresholds != null ? deviceThresholds.get(sensor) : null;
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

    /**
     * Validates all fields: each must be empty (keep current) or a non-negative integer.
     * Disables the save button if any field contains an invalid value.
     */
    private void validateFields() {
        boolean valid = true;
        for (NumberField f : new NumberField[]{warnMin, warnMax, critMin, critMax}) {
            Double val = f.getValue();
            if (val != null && (val < 0 || val > 100 || val != Math.floor(val))) {
                valid = false;
                break;
            }
        }
        saveBtn.setEnabled(valid);
    }

    private void save() {
        String sensor = sensorSelect.getValue();
        String deviceId = deviceSelect.getValue();
        if (sensor == null || deviceId == null) return;

        try {
            AlertThresholdDto dto = new AlertThresholdDto(
                    null, deviceId, sensor,
                    warnMin.getValue(),
                    warnMax.getValue(),
                    critMin.getValue(),
                    critMax.getValue(),
                    null, null);
            alertApiClient.upsertThreshold(dto);

            // Refresh cache for this device
            Map<String, AlertThresholdDto> deviceThresholds = thresholdsByDevice.get(deviceId);
            if (deviceThresholds == null) {
                deviceThresholds = new HashMap<>();
                thresholdsByDevice.put(deviceId, deviceThresholds);
            }
            deviceThresholds.put(sensor, dto);

            // Use simple Notification.show for proper auto-close behavior
            Notification n = Notification.show(
                getTranslation("threshold.save.success"),
                2000,
                Notification.Position.BOTTOM_END
            );
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            Notification n = Notification.show(
                getTranslation("common.error.api"),
                2000,
                Notification.Position.BOTTOM_END
            );
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        buildUI();
    }
}
