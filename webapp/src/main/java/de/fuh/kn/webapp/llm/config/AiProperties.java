package de.fuh.kn.webapp.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfigurations-Properties für die AI-Integration.
 * Liest Werte aus app.ai.* in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

    private ModelConfig evaluation = new ModelConfig();
    private ModelConfig explanation = new ModelConfig();
    private ModelConfig relevanceCheck = new ModelConfig("gpt-4.1-nano", 0.0, 100);
    private ModelConfig pdfImport = new ModelConfig("gpt-4.1", 0.2, 2000);

    /**
     * Konfiguration für ein LLM-Modell.
     */
    @Getter
    @Setter
    public static class ModelConfig {
        /**
         * Der Name des zu verwendenden Modells.
         */
        private String model = "gpt-4.1";
        
        /**
         * Die Temperatur für die Antwortgenerierung (0.0 - 1.0).
         * Niedrigere Werte führen zu deterministischeren Antworten.
         */
        private Double temperature = 0.0;
        
        /**
         * Die maximale Anzahl an generierten Tokens pro Anfrage.
         */
        private Integer maxTokens = 500;
        
        /**
         * Standardkonstruktor.
         */
        public ModelConfig() {
            // Standard-Werte werden durch Standardattribute gesetzt
        }
        
        /**
         * Konstruktor mit angepassten Werten.
         *
         * @param model Das zu verwendende Modell
         * @param temperature Die Temperatur für die Antwortgenerierung
         * @param maxTokens Die maximale Anzahl an generierten Tokens
         */
        public ModelConfig(String model, Double temperature, Integer maxTokens) {
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
    }
}