package de.fuh.kn.webapp.llm.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicConnectionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

/**
 * Konfigurationsklasse für die LLM-Integration.
 * Stellt die benötigten Beans für die Kommunikation mit dem OpenAI-API bereit.
 */
@Configuration
@EnableConfigurationProperties
public class LlmConfig {
    
    private final AiProperties aiProperties;
    
    public LlmConfig(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /**
     * Konfiguriert ein ChatModel für kleinere Evaluierungsaufgaben.
     *
     * @param retryTemplate RetryTemplate für API-Aufrufe
     * @param connectionProperties OpenAI Connection Properties
     * @param observationRegistry ObservationRegistry für Metriken
     * @return Ein ChatModel mit niedriger Temperatur für konsistente Evaluierungen
     */
    @Bean(name = "evaluationChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
    public ChatModel evaluationChatClientOpenAi(RetryTemplate retryTemplate,
                                                OpenAiConnectionProperties connectionProperties,
                                                ObservationRegistry observationRegistry) {

        // Niedrigere Temperatur für konsistentere Evaluierungen
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.getEvaluation().getModel())
                .temperature(aiProperties.getEvaluation().getTemperature())
                .maxTokens(aiProperties.getEvaluation().getMaxTokens())
                .build();

        // Create OpenAI API client
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "evaluationChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "anthropic")
    public ChatModel evaluationChatClientAnthropic(RetryTemplate retryTemplate,
                                                AnthropicConnectionProperties connectionProperties,
                                                ObservationRegistry observationRegistry) {

        // Niedrigere Temperatur für konsistentere Evaluierungen
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(aiProperties.getEvaluation().getModel())
                .temperature(aiProperties.getEvaluation().getTemperature())
                .maxTokens(aiProperties.getEvaluation().getMaxTokens())
                .build();

        // Create OpenAI API client
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }

    /**
     * Konfiguriert ein ChatModel für umfangreichere Erklärungsaufgaben.
     *
     * @param retryTemplate RetryTemplate für API-Aufrufe
     * @param connectionProperties OpenAI Connection Properties
     * @param observationRegistry ObservationRegistry für Metriken
     * @return Ein ChatModel mit mehr verfügbaren Tokens
     */
    @Bean(name = "explanationChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
    public ChatModel explanationChatClientOpenAI(RetryTemplate retryTemplate,
                                          OpenAiConnectionProperties connectionProperties,
                                          ObservationRegistry observationRegistry) {

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.getExplanation().getModel())
                .temperature(aiProperties.getExplanation().getTemperature())
                .maxTokens(aiProperties.getExplanation().getMaxTokens())
                .build();
        
        // Create OpenAI API client
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "explanationChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "anthropic")
    public ChatModel explanationChatClientAnthropic(RetryTemplate retryTemplate,
                                           AnthropicConnectionProperties connectionProperties,
                                           ObservationRegistry observationRegistry) {

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(aiProperties.getExplanation().getModel())
                .temperature(aiProperties.getExplanation().getTemperature())
                .maxTokens(aiProperties.getExplanation().getMaxTokens())
                .build();

        // Create OpenAI API client
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }
    
    /**
     * Konfiguriert ein ChatModel für die Relevanzprüfung von Dokumenten im RAG-Prozess.
     * <p>
     * Diese Konfiguration nutzt ein kleineres Modell mit geringerer Temperatur für
     * deterministische Ja/Nein-Entscheidungen bei der Dokumentrelevanz.
     *
     * @param retryTemplate RetryTemplate für API-Aufrufe
     * @param connectionProperties OpenAI Connection Properties
     * @param observationRegistry ObservationRegistry für Metriken
     * @return Ein ChatModel für effiziente und kostengünstige Relevanzprüfungen
     */
    @Bean(name = "relevanceCheckChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
    public ChatModel relevanceCheckChatClientOpenAI(RetryTemplate retryTemplate,
                                             OpenAiConnectionProperties connectionProperties,
                                             ObservationRegistry observationRegistry) {

        // Niedrige Temperatur für konsistente Ja/Nein-Antworten
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.getRelevanceCheck().getModel())
                .temperature(aiProperties.getRelevanceCheck().getTemperature())
                .maxTokens(aiProperties.getRelevanceCheck().getMaxTokens())
                .build();
        
        // Create OpenAI API client
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "relevanceCheckChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "anthropic")
    public ChatModel relevanceCheckChatClientAnthropic(RetryTemplate retryTemplate,
                                              AnthropicConnectionProperties connectionProperties,
                                              ObservationRegistry observationRegistry) {

        // Niedrige Temperatur für konsistente Ja/Nein-Antworten
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(aiProperties.getRelevanceCheck().getModel())
                .temperature(aiProperties.getRelevanceCheck().getTemperature())
                .maxTokens(aiProperties.getRelevanceCheck().getMaxTokens())
                .build();

        // Create OpenAI API client
        AnthropicApi openAiApi = AnthropicApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }
    
    /**
     * Konfiguriert ein ChatModel für die Extraktion von Aufgaben aus PDF-Dateien.
     * <p>
     * Diese Konfiguration nutzt das GPT-4o Modell mit speziellen Optionen für die Verarbeitung
     * von visuellen Inhalten wie PDFs. Dies erlaubt die Extraktion strukturierter Daten 
     * aus unstrukturierten PDF-Dokumenten.
     *
     * @param retryTemplate RetryTemplate für API-Aufrufe
     * @param connectionProperties OpenAI Connection Properties
     * @param observationRegistry ObservationRegistry für Metriken
     * @return Ein ChatModel für die PDF-Verarbeitung mit Vision-Unterstützung
     */
    @Bean(name = "pdfImportChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
    public ChatModel pdfImportChatClientOpenAI(RetryTemplate retryTemplate,
                                       OpenAiConnectionProperties connectionProperties,
                                       ObservationRegistry observationRegistry) {

        // Konfiguration für GPT-4.1 mit Vision-Unterstützung
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.getPdfImport().getModel())
                .temperature(aiProperties.getPdfImport().getTemperature())
                .maxTokens(aiProperties.getPdfImport().getMaxTokens())
                .build();
        
        // Create OpenAI API client
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "pdfImportChatClient")
    @ConditionalOnProperty(value = "spring.ai.model.chat", havingValue = "anthropic")
    public ChatModel pdfImportChatClientAnthropic(RetryTemplate retryTemplate,
                                         AnthropicConnectionProperties connectionProperties,
                                         ObservationRegistry observationRegistry) {

        // Konfiguration für GPT-4.1 mit Vision-Unterstützung
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(aiProperties.getPdfImport().getModel())
                .temperature(aiProperties.getPdfImport().getTemperature())
                .maxTokens(aiProperties.getPdfImport().getMaxTokens())
                .build();

        // Create OpenAI API client
        AnthropicApi openAiApi = AnthropicApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(openAiApi)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .defaultOptions(options)
                .build();
    }
}