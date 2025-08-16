package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.llm.dto.generator.LlmGeneratorRequestDto;
import de.fuh.kn.webapp.llm.dto.generator.LlmGeneratorResponseDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.rag.modules.GeneratorContextQueryAugmenter;
import de.fuh.kn.webapp.llm.rag.modules.LlmRelevanceFilter;
import de.fuh.kn.webapp.llm.rag.modules.VektorSpeicherDocumentRetriever;
import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import de.fuh.kn.webapp.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service für die LLM-basierte Generierung von Aufgaben.
 * Implementiert die Kernlogik für die RAG-basierte Aufgabengenerierung
 * über Spring AI mit Nutzung von Kursmaterialien und Beispielaufgaben.
 */
@Service
@Slf4j
public class LlmAufgabenGeneratorService {

    private final ChatModel aufgabenGeneratorChatModel;
    private final TokenUsageObserver tokenUsageObserver;
    private final VektorSpeicherService vektorSpeicherService;
    private final LlmRelevanceFilter llmRelevanceFilter;
    
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public LlmAufgabenGeneratorService(@Qualifier("pdfImportChatClient") ChatModel aufgabenGeneratorChatModel, TokenUsageObserver tokenUsageObserver, VektorSpeicherService vektorSpeicherService, LlmRelevanceFilter llmRelevanceFilter) {
        this.aufgabenGeneratorChatModel = aufgabenGeneratorChatModel;
        this.tokenUsageObserver = tokenUsageObserver;
        this.vektorSpeicherService = vektorSpeicherService;
        this.llmRelevanceFilter = llmRelevanceFilter;
    }

    /**
     * Erstellt den RetrievalAugmentationAdvisor mit allen benötigten Komponenten.
     * Verwendet den benutzerdefinierten VektorSpeicherDocumentRetriever für die hybride Suche
     * und einen spezialisierten QueryAugmenter für die Aufgabengenerierung.
     * 
     * @return Der konfigurierte RetrievalAugmentationAdvisor
     */
    private RetrievalAugmentationAdvisor createRetrievalAugmentationAdvisor() {
        if (retrievalAugmentationAdvisor == null) {
            // Benutzerdefinierten Document-Retriever für die hybride Suche erstellen
            // Dieser nutzt den VektorSpeicherService und dessen fortschrittliche Suchfunktionen
            DocumentRetriever documentRetriever = new VektorSpeicherDocumentRetriever(vektorSpeicherService)
                    .withSimilarityThreshold(0.65f)
                    .withTopK(10);

            QueryAugmenter queryAugmenter = new GeneratorContextQueryAugmenter();
            
            // Advisor zusammensetzen
            retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(documentRetriever)
                    .queryAugmenter(queryAugmenter)
                    .documentPostProcessors(llmRelevanceFilter)
                    .build();
        }
        return retrievalAugmentationAdvisor;
    }

    /**
     * Generiert eine neue Aufgabe basierend auf der Anfrage mit Thema und Kontext.
     * Verwendet RAG (Retrieval Augmented Generation) mit dem Spring AI RetrievalAugmentationAdvisor-Pattern
     * zur Erstellung der Aufgabe basierend auf relevanten Kursmaterialien und Beispielaufgaben.
     *
     * @param request Die Anfrage mit Thema, Kurs-Kontext und Beispielaufgaben
     * @return Die generierte Aufgabe mit Metadaten und Kosteninfo
     */
    public LlmGeneratorResponseDto generiereAufgabe(LlmGeneratorRequestDto request) {
        try {
            // Kostentracking starten
            tokenUsageObserver.startCostContext(OperationType.OTHER);
            
            // Filter für die Aufgabe erstellen und im Context übergeben
            String filterExpression = "kursId == " + request.getKursId();
            
            // ChatClient-Prompt mit Aufgabenkontext erstellen
            ChatClient.Builder chatClientBuilder = ChatClient.builder(aufgabenGeneratorChatModel);
            ChatClient chatClient = chatClientBuilder.build();
            
            // Advisor-Kontext erstellen
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .advisors(createRetrievalAugmentationAdvisor())
                    .advisors(a -> {
                        a.param("FILTER_EXPRESSION", filterExpression);
                        a.param("BEISPIELAUFGABEN", request.getBeispielaufgaben());
                        a.param("KURSEINHEIT", request.getKurseinheit());
                    })
                    .user(request.getThema());

            ChatResponse chatResponse = requestSpec
                    .call()
                    .chatResponse();
            
            String responseText = chatResponse.getResult().getOutput().getText();
            
            // Backslashes in LaTeX-Ausdrücken für JSON escapen
            String sanitizedResponseText = JsonUtil.sanitizeJsonResponse(responseText);
            
            // Strukturierte Ausgabe konvertieren
            BeanOutputConverter<AufgabeDto> converter = new BeanOutputConverter<>(AufgabeDto.class);
            AufgabeDto aufgabeDto = converter.convert(sanitizedResponseText);

            LlmGeneratorResponseDto result = new LlmGeneratorResponseDto();
            result.setAufgabe(aufgabeDto);

            // Token-Informationen erfassen
            result.setInputTokens(chatResponse.getMetadata().getUsage().getPromptTokens());
            result.setOutputTokens(chatResponse.getMetadata().getUsage().getCompletionTokens());
            result.setModel(chatResponse.getMetadata().getModel());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            BigDecimal totalCost = costContext.getTotalCost();
            result.setCost(totalCost);
            
            log.info("Aufgabengenerierung durchgeführt mit {} Input-Token und {} Output-Token. Kosten: ${}",
                    costContext.getInputTokens(),
                    costContext.getOutputTokens(),
                    totalCost.toPlainString());

            return result;
            
        } catch (Exception e) {
            log.error("Fehler bei der LLM-basierten Aufgabengenerierung: {}", e.getMessage(), e);
            
            // Fallback-Antwort erstellen
            LlmGeneratorResponseDto fallbackResponse = new LlmGeneratorResponseDto();
            fallbackResponse.setErrorMessage("Fehler bei der Aufgabengenerierung: " + e.getMessage());
            fallbackResponse.setInputTokens(0);
            fallbackResponse.setOutputTokens(0);
            fallbackResponse.setCost(BigDecimal.ZERO);
            
            return fallbackResponse;
        }
    }

}