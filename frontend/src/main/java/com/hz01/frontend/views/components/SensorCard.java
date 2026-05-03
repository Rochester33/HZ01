package com.hz01.frontend.views.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class SensorCard extends Div {

    private final Span valueLabel = new Span();
    private final ProgressBar progressBar = new ProgressBar(0, 1);
    private final Span statusDot = new Span();

    private Double warningMax;
    private Double criticalMax;
    private Double warningMin;
    private Double criticalMin;

    public SensorCard(String title, String unit) {
        addClassName("sensor-card");

        Span titleLabel = new Span(title);
        titleLabel.addClassName("sensor-card-title");

        Span unitLabel = new Span(unit);
        unitLabel.addClassName("sensor-card-unit");

        valueLabel.addClassName("sensor-card-value");
        valueLabel.setText("--");

        statusDot.addClassName("sensor-card-dot");

        Div valueRow = new Div(valueLabel, unitLabel, statusDot);
        valueRow.addClassName("sensor-card-value-row");

        progressBar.addClassName("sensor-card-bar");
        progressBar.setVisible(false);

        VerticalLayout layout = new VerticalLayout(titleLabel, valueRow, progressBar);
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);
    }

    public void update(Double value) {
        if (value == null) {
            valueLabel.setText("--");
            removeClassNames("sensor-warning", "sensor-critical", "sensor-normal");
            progressBar.setVisible(false);
            return;
        }

        valueLabel.setText(String.format("%.1f", value));
        String level = computeLevel(value);
        removeClassNames("sensor-warning", "sensor-critical", "sensor-normal");
        addClassName("sensor-" + level);

        if (criticalMax != null && criticalMax > 0) {
            progressBar.setVisible(true);
            progressBar.setValue(Math.min(value / criticalMax, 1.0));
        }
    }

    private String computeLevel(double value) {
        if ((criticalMin != null && value < criticalMin) || (criticalMax != null && value > criticalMax)) {
            return "critical";
        }
        if ((warningMin != null && value < warningMin) || (warningMax != null && value > warningMax)) {
            return "warning";
        }
        return "normal";
    }

    public void setThresholds(Double warningMin, Double warningMax, Double criticalMin, Double criticalMax) {
        this.warningMin = warningMin;
        this.warningMax = warningMax;
        this.criticalMin = criticalMin;
        this.criticalMax = criticalMax;
    }
}
