package com.hz01.frontend.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Sets English as the default locale for every new Vaadin session,
 * overriding the JVM default locale (which may be Chinese on some hosts).
 */
@Component
public class LocaleInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(sessionEvent ->
                sessionEvent.getSession().setLocale(HZ01I18nProvider.LOCALE_EN)
        );
    }
}
