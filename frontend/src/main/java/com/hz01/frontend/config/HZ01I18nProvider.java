package com.hz01.frontend.config;

import com.vaadin.flow.i18n.I18NProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@Component
public class HZ01I18nProvider implements I18NProvider {

    private static final Logger log = LoggerFactory.getLogger(HZ01I18nProvider.class);

    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_ZH = Locale.SIMPLIFIED_CHINESE;
    public static final Locale LOCALE_RU = Locale.forLanguageTag("ru");
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_DE = Locale.GERMAN;
    public static final Locale LOCALE_IT = Locale.ITALIAN;
    public static final Locale LOCALE_ES = Locale.forLanguageTag("es");

    /** English is first — Vaadin uses the first entry as the application default locale. */
    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(LOCALE_EN, LOCALE_ZH, LOCALE_RU, LOCALE_FR, LOCALE_DE, LOCALE_IT, LOCALE_ES);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = loadBundle(locale);
        if (bundle == null) {
            bundle = loadBundle(LOCALE_EN);
        }
        if (bundle == null) {
            return key;
        }
        try {
            String pattern = bundle.getString(key);
            return params.length > 0 ? MessageFormat.format(pattern, params) : pattern;
        } catch (MissingResourceException e) {
            log.warn("Missing i18n key '{}' for locale {}", key, locale);
            return key;
        }
    }

    /**
     * Loads the bundle by directly reading the properties file so we bypass
     * the JVM ResourceBundle cache and its default-locale interference.
     * Lookup order: messages_<lang>.properties → messages_en.properties → messages.properties
     */
    private ResourceBundle loadBundle(Locale locale) {
        String[] candidates = {
                "i18n/messages_" + locale.getLanguage() + ".properties",
                "i18n/messages_en.properties",
                "i18n/messages.properties"
        };
        for (String path : candidates) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                } catch (IOException e) {
                    log.warn("Failed to read bundle {}: {}", path, e.getMessage());
                }
            }
        }
        log.warn("No bundle found for locale {}", locale);
        return null;
    }
}
