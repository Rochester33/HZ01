package com.hz01.frontend.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Custom notification component that uses JavaScript for reliable positioning and auto-close.
 * Displays notifications at BOTTOM_START (left bottom) and stacks them vertically upward.
 */
public class CustomNotification {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Show a notification with automatic positioning and auto-close.
     *
     * @param message  The message to display
     * @param duration Duration in milliseconds (e.g., 3000 for 3 seconds)
     * @param type     Notification type (SUCCESS, ERROR, WARNING, INFO)
     */
    public static void show(String message, int duration, Type type) {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        // Map type to CSS class and color
        String typeClass;
        String color;
        switch (type) {
            case SUCCESS:
                typeClass = "success";
                color = "#22c55e";
                break;
            case ERROR:
                typeClass = "error";
                color = "#dc2626";
                break;
            case WARNING:
                typeClass = "warning";
                color = "#f59e0b";
                break;
            default:
                typeClass = "info";
                color = "#fbbf24";
        }

        // Execute JavaScript to create and manage notification
        ui.getPage().executeJs(
            "const container = document.getElementById('custom-notification-container') || (() => {" +
            "  const c = document.createElement('div');" +
            "  c.id = 'custom-notification-container';" +
            "  c.style.cssText = 'position: fixed; bottom: 20px; left: 20px; z-index: 10000; display: flex; flex-direction: column-reverse; gap: 12px; pointer-events: none;';" +
            "  document.body.appendChild(c);" +
            "  return c;" +
            "})();" +
            "" +
            "const notif = document.createElement('div');" +
            "notif.style.cssText = 'background: #000000; border: 3px solid ' + $0 + '; box-shadow: 0 0 0 3px ' + $0 + ', 0 8px 32px rgba(0,0,0,0.9); padding: 0; display: flex; align-items: stretch; animation: slideInLeft 0.3s ease-out; pointer-events: auto; max-width: 400px; position: relative;';" +
            "" +
            "const stripe = document.createElement('div');" +
            "stripe.style.cssText = 'width: 8px; background: repeating-linear-gradient(45deg, ' + $0 + ', ' + $0 + ' 10px, #000000 10px, #000000 20px); flex-shrink: 0;';" +
            "" +
            "const content = document.createElement('div');" +
            "content.style.cssText = 'padding: 14px 24px; color: ' + $0 + '; font-family: \"JetBrains Mono\", monospace; font-size: 0.875rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; line-height: 1.4;';" +
            "content.textContent = $1;" +
            "" +
            "notif.appendChild(stripe);" +
            "notif.appendChild(content);" +
            "container.appendChild(notif);" +
            "" +
            "setTimeout(() => {" +
            "  notif.style.animation = 'slideOutLeft 0.3s ease-out';" +
            "  setTimeout(() => notif.remove(), 300);" +
            "}, $2);" +
            "" +
            "if (!document.getElementById('custom-notification-styles')) {" +
            "  const style = document.createElement('style');" +
            "  style.id = 'custom-notification-styles';" +
            "  style.textContent = '@keyframes slideInLeft { from { opacity: 0; transform: translateX(-100px); } to { opacity: 1; transform: translateX(0); } } @keyframes slideOutLeft { from { opacity: 1; transform: translateX(0); } to { opacity: 0; transform: translateX(-100px); } }';" +
            "  document.head.appendChild(style);" +
            "}",
            color, message, duration
        );
    }

    /**
     * Show a success notification.
     */
    public static void showSuccess(String message) {
        show(message, 3000, Type.SUCCESS);
    }

    /**
     * Show an error notification.
     */
    public static void showError(String message) {
        show(message, 3000, Type.ERROR);
    }

    /**
     * Show a warning notification.
     */
    public static void showWarning(String message) {
        show(message, 3000, Type.WARNING);
    }

    /**
     * Show an info notification.
     */
    public static void showInfo(String message) {
        show(message, 3000, Type.INFO);
    }
}
