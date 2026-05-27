package com.hz01.frontend.config;

import com.vaadin.flow.i18n.I18NProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Component
public class HZ01I18nProvider implements I18NProvider {

    private static final Logger log = LoggerFactory.getLogger(HZ01I18nProvider.class);

    public static final Locale LOCALE_ZH = Locale.SIMPLIFIED_CHINESE;
    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_RU = Locale.forLanguageTag("ru");
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_DE = Locale.GERMAN;
    public static final Locale LOCALE_IT = Locale.ITALIAN;
    public static final Locale LOCALE_ES = Locale.forLanguageTag("es");

    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(LOCALE_ZH, LOCALE_EN, LOCALE_RU, LOCALE_FR, LOCALE_DE, LOCALE_IT, LOCALE_ES);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("i18n/messages", locale);
        } catch (MissingResourceException e) {
            log.warn("Missing i18n bundle for locale {}", locale);
            bundle = ResourceBundle.getBundle("i18n/messages", LOCALE_EN);
        }
        try {
            String pattern = bundle.getString(key);
            return params.length > 0 ? MessageFormat.format(pattern, params) : pattern;
        } catch (MissingResourceException e) {
            log.warn("Missing i18n key: {}", key);
            return key;
        }
    }
}
