package com.hz01.frontend.views.components;

import com.vaadin.flow.component.html.Span;

public class StatusBadge extends Span {

    public StatusBadge(String status) {
        update(status);
        addClassName("status-badge");
    }

    public void update(String status) {
        removeClassNames("badge-normal", "badge-warning", "badge-critical", "badge-offline");
        if (status == null) status = "offline";
        addClassName("badge-" + status.toLowerCase());
        setText(status.toUpperCase());
    }
}
