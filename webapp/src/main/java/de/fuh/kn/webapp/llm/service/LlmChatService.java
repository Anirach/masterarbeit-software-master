package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.chat.LlmChatRequestDto;
import de.fuh.kn.webapp.llm.dto.chat.LlmChatResponseDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.rag.modules.AufgabeContextQueryAugmenter;
import de.fuh.kn.webapp.llm.rag.modules.LlmRelevanceFilter;
import de.fuh.kn.webapp.llm.rag.modules.VektorSpeicherDocumentRetriever;
import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service für die LLM-basierte Chat-Funktionalität.
 * Implementiert die Kernlogik für die Generierung von Chat-Antworten
 * über Spring AI mit RAG (Retrieval Augmented Generation).
 */
@Service
@Slf4j
public class LlmChatService {

    private final ChatModel chatModel;
    private final VektorSpeicherService vektorSpeicherService;
    private final LlmRelevanceFilter llmRelevanceFilter;
    private final TokenUsageObserver tokenUsageObserver;
    
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public LlmChatService(@Qualifier("explanationChatClient") ChatModel chatModel, VektorSpeicherService vektorSpeicherService, LlmRelevanceFilter llmRelevanceFilter, TokenUsageObserver tokenUsageObserver) {
        this.chatModel = chatModel;
        this.vektorSpeicherService = vektorSpeicherService;
        this.llmRelevanceFilter = llmRelevanceFilter;
        this.tokenUsageObserver = tokenUsageObserver;
    }

    /**
     * Erstellt den RetrievalAugmentationAdvisor mit allen benötigten Komponenten.
     * Verwendet den benutzerdefinierten VektorSpeicherDocumentRetriever, um die
     * hybride Suchfunktionalität des VektorSpeicherService zu nutzen.
     * 
     * @return Der konfigurierte RetrievalAugmentationAdvisor
     */
    private RetrievalAugmentationAdvisor createRetrievalAugmentationAdvisor() {
        if (retrievalAugmentationAdvisor == null) {
            // Benutzerdefinierten Document-Retriever für die hybride Suche erstellen
            // Dieser nutzt den VektorSpeicherService und dessen fortschrittliche Suchfunktionen
            DocumentRetriever documentRetriever = new VektorSpeicherDocumentRetriever(vektorSpeicherService)
                    .withSimilarityThreshold(0.65f)
                    .withTopK(5);

            QueryAugmenter queryAugmenter = new AufgabeContextQueryAugmenter();
            
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
     * Generiert eine Antwort basierend auf der Chat-Anfrage.
     * Verwendet RAG (Retrieval Augmented Generation) mit dem Spring AI RetrievalAugmentationAdvisor-Pattern
     * zur Erstellung der Antwort basierend auf relevanten Kursmaterialien und Aufgabeninhalten.
     *
     * @param request Die Chat-Anfrage mit allen notwendigen Kontext-Informationen
     * @return Die generierte Antwort mit Metadaten und Referenzen
     */
    public LlmChatResponseDto generiereAntwort(LlmChatRequestDto request) {
        try {
            // Kostentracking starten mit CHAT_MESSAGE-Typ
            tokenUsageObserver.startCostContext(OperationType.CHAT_MESSAGE);
            
            // Filter für die Aufgabe erstellen und im Context übergeben
            String filterExpression = "kursId == " + request.getKursId();
            
            // ChatClient-Prompt mit Aufgabenkontext erstellen
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
            ChatClient chatClient = chatClientBuilder.build();
            
            // Konversationshistorie als Nachrichten aufbauen
            List<Message> conversationHistory = new ArrayList<>();
            
            // Bisherige Chat-Nachrichten als History hinzufügen
            for (LlmChatRequestDto.ChatMessage nachricht : request.getChatHistory()) {
                if (nachricht.isSystemMessage()) {
                    conversationHistory.add(new SystemMessage(nachricht.getContent()));
                } else {
                    conversationHistory.add(new UserMessage(nachricht.getContent()));
                }
            }

            // Advisor-Kontext erstellen
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .advisors(createRetrievalAugmentationAdvisor())
                    .advisors(a -> {
                        a.param("FILTER_EXPRESSION", filterExpression);
                        a.param("AUFGABE", request.getAufgabe());
                        a.param("TEILAUFGABE", request.getTeilaufgabe());
                        a.param("LOESUNGSVERSUCH", request.getLoesungsversuch());
                        a.param("EXPLANATION_MODE", request.isExplanationMode());
                    })
                    .messages(conversationHistory)
                    .user(request.getUserMessage());

            ChatResponse chatResponse = requestSpec
                    .call()
                    .chatResponse();

            // Token-Nutzung aktualisieren und Kosten berechnen
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();

            // Gefundene Dokumente aus der Antwort extrahieren
            List<Document> documents = chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            List<LlmChatResponseDto.DocumentReference> documentReferences = new ArrayList<>();
            
            if (documents != null && !documents.isEmpty()) {
                for (Document document : documents) {
                    if (document.getMetadata().get("kursMaterialId") instanceof Integer kursMaterialId) {
                        LlmChatResponseDto.DocumentReference reference = new LlmChatResponseDto.DocumentReference();
                        reference.setKursMaterialId(Long.valueOf(kursMaterialId));
                        
                        // Seitennummer aus Metadaten extrahieren, falls vorhanden
                        if (document.getMetadata().get("page_number") instanceof Integer pageNumber) {
                            reference.setPageNumber(pageNumber);
                        } else if (document.getMetadata().get("page_number") instanceof String pageStr) {
                            try {
                                reference.setPageNumber(Integer.parseInt(pageStr));
                            } catch (NumberFormatException e) {
                                log.debug("Konnte Seitennummer nicht parsen: {}", pageStr);
                            }
                        }
                        
                        documentReferences.add(reference);
                    }
                }
            }

            // Response DTO erstellen
            LlmChatResponseDto response = new LlmChatResponseDto();
            response.setContent(chatResponse.getResult().getOutput().getText());
            response.setInputTokens(chatResponse.getMetadata().getUsage().getPromptTokens());
            response.setOutputTokens(chatResponse.getMetadata().getUsage().getCompletionTokens());
            response.setModel(chatResponse.getMetadata().getModel());
            response.setCost(costContext.getTotalCost());
            response.setDocumentReferences(documentReferences);

            log.info("Chat-Antwort mit RetrievalAugmentationAdvisor generiert. Modell: {}, Input-Token: {}, Output-Token: {}, Kosten: ${}", 
                    costContext.getModel(),
                    costContext.getInputTokens(),
                    costContext.getOutputTokens(),
                    costContext.getTotalCost().toPlainString());

            return response;
            
        } catch (Exception e) {
            log.error("Fehler bei der Generierung der Chat-Antwort: {}", e.getMessage(), e);
            
            // Fallback-Antwort erstellen
            LlmChatResponseDto fallbackResponse = new LlmChatResponseDto();
            fallbackResponse.setContent("Entschuldigung, bei der Generierung einer Antwort ist ein Fehler aufgetreten. " +
                    "Bitte versuche es später noch einmal oder formuliere deine Frage anders.");
            fallbackResponse.setInputTokens(0);
            fallbackResponse.setOutputTokens(0);
            fallbackResponse.setCost(BigDecimal.ZERO);
            fallbackResponse.setDocumentReferences(new ArrayList<>());
            
            return fallbackResponse;
        }
    }
}