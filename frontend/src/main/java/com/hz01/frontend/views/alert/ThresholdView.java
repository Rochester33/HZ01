package com.hz01.frontend.views.alert;

import com.hz01.frontend.client.AlertApiClient;
import com.hz01.frontend.dto.AlertThresholdDto;
import com.hz01.frontend.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

    // sensor_type -> {field -> NumberField}
    private final Map<String, Map<String, NumberField>> fields = new HashMap<>();

    private static final List<String> SENSORS = List.of(
            "temperature", "humidity", "oxygen", "co_level", "battery_level");

    public ThresholdView(AlertApiClient alertApiClient) {
        this.alertApiClient = alertApiClient;
        setSizeFull();
        setPadding(true);
        buildForm();
    }

    private void buildForm() {
        removeAll();
        fields.clear();

        add(new H2(getTranslation("nav.thresholds")));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));

        List<AlertThresholdDto> thresholds = alertApiClient.getThresholds();
        Map<String, AlertThresholdDto> byType = new HashMap<>();
        thresholds.forEach(t -> byType.put(t.sensorType(), t));

        for (String sensor : SENSORS) {
            AlertThresholdDto t = byType.get(sensor);
            String label = getTranslation("sensor." + sensor.replace("_level", "").replace("co_", "co"));
            String unit = t != null && t.unit() != null ? " (" + t.unit() + ")" : "";

            form.add(new H4(label + unit), 2);

            NumberField warnMin = field(getTranslation("threshold.warning.level") + " Min", t != null ? t.warningMin() : null);
            NumberField warnMax = field(getTranslation("threshold.warning.level") + " Max", t != null ? t.warningMax() : null);
            NumberField critMin = field(getTranslation("threshold.critical.level") + " Min", t != null ? t.criticalMin() : null);
            NumberField critMax = field(getTranslation("threshold.critical.level") + " Max", t != null ? t.criticalMax() : null);

            form.add(warnMin, warnMax, critMin, critMax);
            form.add(new Hr(), 2);

            Map<String, NumberField> sensorFields = new HashMap<>();
            sensorFields.put("warnMin", warnMin);
            sensorFields.put("warnMax", warnMax);
            sensorFields.put("critMin", critMin);
            sensorFields.put("critMax", critMax);
            fields.put(sensor, sensorFields);
        }

        Button save = new Button(getTranslation("threshold.save"), e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(form, new HorizontalLayout(save));
    }

    private NumberField field(String label, Double value) {
        NumberField f = new NumberField(label);
        if (value != null) f.setValue(value);
        f.setClearButtonVisible(true);
        return f;
    }

    private void save() {
        try {
            for (String sensor : SENSORS) {
                Map<String, NumberField> f = fields.get(sensor);
                AlertThresholdDto dto = new AlertThresholdDto(
                        null, null, sensor,
                        f.get("warnMin").getValue(),
                        f.get("warnMax").getValue(),
                        f.get("critMin").getValue(),
                        f.get("critMax").getValue(),
                        null, null);
                alertApiClient.upsertThreshold(dto);
            }
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
        buildForm();
    }
}
