package com.hz01.frontend.views;

import com.hz01.frontend.config.HZ01I18nProvider;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.hz01.frontend.views.dashboard.DashboardView;
import com.hz01.frontend.views.alert.AlertView;
import com.hz01.frontend.views.alert.ThresholdView;
import com.hz01.frontend.views.control.ControlView;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainLayout extends AppLayout implements RouterLayout, LocaleChangeObserver {

    private final SideNavItem navDashboard  = new SideNavItem("", DashboardView.class);
    private final SideNavItem navAlerts     = new SideNavItem("", AlertView.class);
    private final SideNavItem navThresholds = new SideNavItem("", ThresholdView.class);
    private final SideNavItem navControl    = new SideNavItem("", ControlView.class);

    private static final Map<String, Locale> LOCALES = new LinkedHashMap<>();

    static {
        LOCALES.put("English",  HZ01I18nProvider.LOCALE_EN);
        LOCALES.put("中文",     HZ01I18nProvider.LOCALE_ZH);
        LOCALES.put("Русский",  HZ01I18nProvider.LOCALE_RU);
        LOCALES.put("Français", HZ01I18nProvider.LOCALE_FR);
        LOCALES.put("Deutsch",  HZ01I18nProvider.LOCALE_DE);
        LOCALES.put("Italiano", HZ01I18nProvider.LOCALE_IT);
        LOCALES.put("Español",  HZ01I18nProvider.LOCALE_ES);
    }

    public MainLayout() {
        buildDrawer();
        buildNavbar();
    }

    private void buildDrawer() {
        Div accentBar = new Div();
        accentBar.getStyle()
                .set("height", "6px")
                .set("width", "100%")
                .set("background", "repeating-linear-gradient(90deg, #fbbf24 0px, #fbbf24 20px, #000000 20px, #000000 40px)");

        Span watermark = new Span("HZ-01");
        watermark.addClassName("drawer-watermark");

        SideNav nav = new SideNav();
        nav.addItem(navDashboard, navAlerts, navThresholds, navControl);
        nav.getStyle().set("padding", "0.5rem 0.5rem");
        nav.setWidthFull();

        addToDrawer(accentBar, watermark, nav);
    }

    private void buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle().set("color", "#fbbf24");

        // Brand — absolutely centered in the navbar
        Div brand = new Div();
        brand.addClassName("nav-brand");
        brand.getStyle()
                .set("position", "absolute")
                .set("left", "50%")
                .set("top", "50%")
                .set("transform", "translate(-50%, -50%)")
                .set("text-align", "center");

        Span brandTitle = new Span("HZ-01");
        brandTitle.addClassName("brand-title");

        Span versionBadge = new Span("V0.01");
        versionBadge.addClassName("brand-version");

        Div titleRow = new Div(brandTitle, versionBadge);
        titleRow.addClassName("brand-title-row");

        Span brandSub = new Span("HAZARD MONITORING SYSTEM");
        brandSub.addClassName("brand-sub");

        brand.add(titleRow, brandSub);

        // Language selector — right side
        // Default to English if no session locale is set or if it's the JVM default
        Locale sessionLocale = VaadinSession.getCurrent() != null
                ? VaadinSession.getCurrent().getLocale()
                : HZ01I18nProvider.LOCALE_EN;

        // If the session locale doesn't match any of our supported locales, fall back to English
        String currentLang = LOCALES.entrySet().stream()
                .filter(e -> e.getValue().getLanguage().equals(sessionLocale.getLanguage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("English");

        Select<String> langSelect = new Select<>();
        langSelect.setItems(LOCALES.keySet().stream().toList());
        langSelect.setValue(currentLang);
        langSelect.getStyle()
                .set("--lumo-base-color", "#000000")
                .set("--lumo-body-text-color", "#fbbf24");
        langSelect.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                Locale locale = LOCALES.getOrDefault(e.getValue(), HZ01I18nProvider.LOCALE_EN);
                VaadinSession.getCurrent().setLocale(locale);
                UI.getCurrent().getPage().reload();
            }
        });

        Div rightSection = new Div(langSelect);
        rightSection.getStyle()
                .set("margin-left", "auto")
                .set("display", "flex")
                .set("align-items", "center");

        // Wrapper: position:relative so the brand can use position:absolute
        Div navWrapper = new Div(toggle, brand, rightSection);
        navWrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "100%")
                .set("position", "relative")
                .set("padding", "0 1rem")
                .set("box-sizing", "border-box");

        addToNavbar(navWrapper);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        navDashboard.setLabel(getTranslation("nav.dashboard"));
        navAlerts.setLabel(getTranslation("nav.alerts"));
        navThresholds.setLabel(getTranslation("nav.thresholds"));
        navControl.setLabel(getTranslation("nav.control"));
    }
}
