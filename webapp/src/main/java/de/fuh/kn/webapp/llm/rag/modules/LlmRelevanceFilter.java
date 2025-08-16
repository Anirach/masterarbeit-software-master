package de.fuh.kn.webapp.llm.rag.modules;

import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.rag.config.RagConfig.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ein DocumentPostProcessor, der LLMs nutzt, um die Relevanz von Dokumenten für eine Anfrage zu bewerten.
 * <p>
 * Diese Komponente:
 * <ul>
 *   <li>Filtert irrelevante Dokumente aus den Suchergebnissen</li>
 *   <li>Verwendet ein kompaktes LLM (GPT-4-nano), um Dokumente kostengünstig zu bewerten</li>
 *   <li>Verarbeitet Dokumente parallel für optimale Performance</li>
 *   <li>Integriert sich in den Spring AI RAG-Workflow</li>
 * </ul>
 */
@Component
@Slf4j
public class LlmRelevanceFilter implements DocumentPostProcessor {

    private final ChatModel relevanceCheckChatModel;
    private final TokenUsageObserver tokenUsageObserver;
    private final ExecutorService executorService;
    private final RagProperties ragProperties;
    
    /**
     * System-Prompt für die Relevanzprüfung.
     * Weist das LLM an, Dokumente nach ihrer Relevanz für eine Frage zu bewerten.
     */
    private static final String SYSTEM_PROMPT = 
            "Du bist ein KI-Assistent, der Dokumente auf ihre Relevanz für eine Frage prüft.\n" +
            "Bewerte, ob das gegebene Dokument zur Beantwortung der Frage beitragen kann.\n" +
            "Antworte nur mit JA oder NEIN.";
    
    /**
     * Benutzer-Prompt-Template für die Relevanzbewertung.
     * Wird mit der Frage und dem Dokumenteninhalt gefüllt.
     */
    private static final String USER_PROMPT_TEMPLATE = 
            "Frage: %s\n\n" +
            "Dokument: %s\n\n" +
            "Ist dieses Dokument relevant für die Beantwortung der Frage? Antworte nur mit JA oder NEIN.";
    
    /**
     * Konstruktor für den LlmRelevanceFilter.
     *
     * @param relevanceCheckChatModel Das zu verwendende ChatModel für Relevanzbewertungen
     * @param tokenUsageObserver Der TokenUsageObserver für Kostentracking
     * @param ragProperties Die RAG-Konfigurationseigenschaften
     */
    public LlmRelevanceFilter(
            @Qualifier("relevanceCheckChatClient") ChatModel relevanceCheckChatModel,
            TokenUsageObserver tokenUsageObserver,
            RagProperties ragProperties) {
        this.relevanceCheckChatModel = relevanceCheckChatModel;
        this.tokenUsageObserver = tokenUsageObserver;
        this.ragProperties = ragProperties;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    /**
     * Verarbeitet die abgerufenen Dokumente und filtert irrelevante basierend auf LLM-Bewertung.
     * Der Filter wird nur angewendet, wenn er aktiviert ist und die Mindestanzahl von Dokumenten erreicht wird.
     *
     * @param query Die ursprüngliche Anfrage
     * @param documents Die abgerufenen Dokumente
     * @return Eine gefilterte Liste von Dokumenten, die als relevant eingestuft wurden
     */
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents.isEmpty()) {
            return documents;
        }
        
        // Prüfen, ob der Filter aktiviert ist und die Mindestanzahl von Dokumenten erreicht wird
        if (!ragProperties.getRelevanceFilter().isEnabled() || 
            documents.size() < ragProperties.getRelevanceFilter().getMinDocumentCount()) {
            log.debug("LlmRelevanceFilter übersprungen: Filter {} oder zu wenige Dokumente ({} < {})", 
                    ragProperties.getRelevanceFilter().isEnabled() ? "aktiviert" : "deaktiviert",
                    documents.size(), 
                    ragProperties.getRelevanceFilter().getMinDocumentCount());
            return documents;
        }
        
        // Starte Kostentracking mit Operationstyp
        tokenUsageObserver.startCostContext(OperationType.OTHER);

        log.debug("LlmRelevanceFilter: Bewerte {} Dokumente auf Relevanz für: '{}'", 
                documents.size(), query.text());
        
        // Wähle die Verarbeitungsmethode basierend auf der Konfiguration
        List<Document> relevantDocuments;
        if (ragProperties.getRelevanceFilter().isParallelProcessing()) {
            relevantDocuments = filterRelevantDocumentsConcurrently(query.text(), documents);
        } else {
            relevantDocuments = filterRelevantDocumentsSequentially(query.text(), documents);
        }
        
        TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
        log.info("LlmRelevanceFilter: {}/{} Dokumente als relevant bewertet. Kosten: ${}", 
                relevantDocuments.size(), documents.size(), costContext.getTotalCost().toPlainString());
        
        return relevantDocuments;
    }
    
    /**
     * Bewertet Dokumente parallel auf ihre Relevanz zur Anfrage.
     *
     * @param queryText Der Anfragetext
     * @param documents Die zu bewertenden Dokumente
     * @return Eine Liste der als relevant eingestuften Dokumente
     */
    private List<Document> filterRelevantDocumentsConcurrently(String queryText, List<Document> documents) {
        List<CompletableFuture<Boolean>> relevanceFutures = new ArrayList<>();
        
        // Erstelle Future für jedes Dokument
        for (Document document : documents) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
                    () -> isDocumentRelevant(queryText, document),
                    executorService
            );
            relevanceFutures.add(future);
        }
        
        // Warte auf alle Futures und sammle relevante Dokumente
        List<Document> relevantDocuments = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            try {
                if (relevanceFutures.get(i).get()) {
                    relevantDocuments.add(documents.get(i));
                }
            } catch (Exception e) {
                log.error("Fehler bei der Relevanzprüfung für Dokument {}: {}", i, e.getMessage());
                // Bei Fehler Dokument als relevant betrachten
                relevantDocuments.add(documents.get(i));
            }
        }
        
        return relevantDocuments;
    }
    
    /**
     * Bewertet Dokumente sequentiell auf ihre Relevanz zur Anfrage.
     *
     * @param queryText Der Anfragetext
     * @param documents Die zu bewertenden Dokumente
     * @return Eine Liste der als relevant eingestuften Dokumente
     */
    private List<Document> filterRelevantDocumentsSequentially(String queryText, List<Document> documents) {
        List<Document> relevantDocuments = new ArrayList<>();
        
        for (Document document : documents) {
            if (isDocumentRelevant(queryText, document)) {
                relevantDocuments.add(document);
            }
        }
        
        return relevantDocuments;
    }
    
    /**
     * Überprüft die Relevanz eines einzelnen Dokuments mit dem LLM.
     *
     * @param queryText Der Anfragetext
     * @param document Das zu prüfende Dokument
     * @return true, wenn das Dokument als relevant eingestuft wird, sonst false
     */
    private boolean isDocumentRelevant(String queryText, Document document) {
        try {
            // Prompt erstellen
            String userPromptText = String.format(USER_PROMPT_TEMPLATE, queryText, document.getText());
            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userPromptText))
            );
            
            // LLM-Antwort abrufen
            String response = relevanceCheckChatModel.call(prompt).getResult().getOutput().getText().trim().toUpperCase();
            
            // Antwort auswerten
            boolean isRelevant = response.contains("JA");
            
            log.debug("Dokument Relevanz-Check: {} für '{}...'", 
                    isRelevant ? "RELEVANT" : "NICHT RELEVANT", 
                    document.getText().substring(0, Math.min(50, document.getText().length())) + "...");
            
            return isRelevant;
        } catch (Exception e) {
            log.error("Fehler bei der Relevanzprüfung: {}", e.getMessage());
            // Bei Fehler Dokument als relevant betrachten
            return true;
        }
    }
}