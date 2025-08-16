package de.fuh.kn.webapp.llm.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Konfigurationsklasse für die RAG-Komponenten (Retrieval-Augmented Generation).
 * Stellt benötigte Beans und Properties für die Wissensbasis und Vektorsuche bereit.
 */
@Configuration
@EnableAsync
public class RagConfig {
    
    /**
     * Konfiguriert einen asynchronen Task-Executor für die parallele Verarbeitung von Dokumenten.
     *
     * @return Ein konfigurierter ThreadPoolTaskExecutor
     */
    @Bean("ragTaskExecutor")
    public AsyncTaskExecutor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("rag-");
        executor.initialize();
        return executor;
    }
    
    /**
     * Properties-Klasse für RAG-spezifische Konfigurationen.
     */
    @Setter
    @Getter
    @ConfigurationProperties(prefix = "app.ai.rag")
    public static class RagProperties {
        private int chunkSize = 500;
        private int chunkOverlap = 20;
        private int defaultTopK = 5;
        private float defaultSimilarityThreshold = 0.7f;
        private RelevanceFilterProperties relevanceFilter = new RelevanceFilterProperties();
        private RelevanceBoostProperties relevanceBoost = new RelevanceBoostProperties();
        
        /**
         * Konfigurationseigenschaften für den LLM-basierten Relevanzfilter.
         */
        @Setter
        @Getter
        public static class RelevanceFilterProperties {
            /**
             * Aktiviert oder deaktiviert den Relevanzfilter.
             */
            private boolean enabled = true;
            
            /**
             * Aktiviert oder deaktiviert die parallele Verarbeitung von Dokumenten.
             */
            private boolean parallelProcessing = true;
            
            /**
             * Die Mindestanzahl von Dokumenten, ab der der Relevanzfilter angewendet wird.
             * Bei weniger Dokumenten wird kein Filtern durchgeführt.
             */
            private int minDocumentCount = 3;
        }
        
        /**
         * Konfigurationseigenschaften für die Relevanz-Boost-Funktionalität.
         */
        @Setter
        @Getter
        public static class RelevanceBoostProperties {
            /**
             * Konfiguration für den Boost von Dokumenten aus derselben Kurseinheit.
             */
            private SameKurseinheitBoost sameKurseinheit = new SameKurseinheitBoost();
            
            @Setter
            @Getter
            public static class SameKurseinheitBoost {
                /**
                 * Aktiviert oder deaktiviert den Boost für Dokumente aus derselben Kurseinheit.
                 */
                private boolean enabled = true;
                
                /**
                 * Der Boost-Faktor für Dokumente aus derselben Kurseinheit.
                 * Wird zum Score addiert (z.B. 0.3 = 30% Bonus).
                 */
                private float boostFactor = 0.3f;
            }
        }
    }
    
    /**
     * Bean für die RAG-Properties.
     *
     * @return Die RAG-Properties
     */
    @Bean
    public RagProperties ragProperties() {
        return new RagProperties();
    }
}