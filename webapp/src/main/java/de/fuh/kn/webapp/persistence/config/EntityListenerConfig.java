package de.fuh.kn.webapp.persistence.config;

import de.fuh.kn.webapp.persistence.listener.KursMaterialEntityListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Konfigurationsklasse für Entity Listeners.
 * Stellt sicher, dass die Entity Listeners als Spring Beans registriert werden.
 */
@Configuration
public class EntityListenerConfig {

    /**
     * Registriert den KursMaterialEntityListener als Spring Bean.
     * 
     * @return Die KursMaterialEntityListener-Instanz
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public KursMaterialEntityListener kursMaterialEntityListener() {
        return new KursMaterialEntityListener();
    }
}