package de.fuh.kn.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Konfiguration für Internationalisierung (i18n).
 * Ermöglicht das Wechseln zwischen Deutsch und Englisch.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    /**
     * Konfiguriert den LocaleResolver für Sessions.
     * Standardsprache ist Deutsch.
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.GERMAN);
        return slr;
    }

    /**
     * Interceptor zum Ändern der Sprache über URL-Parameter 'lang'.
     * Beispiel: ?lang=en für Englisch, ?lang=de für Deutsch
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}