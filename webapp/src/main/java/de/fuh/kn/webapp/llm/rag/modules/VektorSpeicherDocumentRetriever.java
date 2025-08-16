package de.fuh.kn.webapp.llm.rag.modules;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.List;

/**
 * Ein benutzerdefinierter DocumentRetriever, der den VektorSpeicherService für die Dokumentsuche verwendet.
 * <p>
 * Diese Implementierung ermöglicht die Nutzung der fortschrittlichen hybriden Suche des VektorSpeicherService
 * innerhalb des Spring AI RAG-Frameworks. Sie bietet folgende Vorteile:
 * <ul>
 *   <li>Kombinierte Vektor- und Textsuche für präzisere Ergebnisse</li>
 *   <li>Filterung von Dokumenten basierend auf Metadaten</li>
 *   <li>TF-IDF-basierte Textrelevanzberechnung</li>
 *   <li>Flexible Konfiguration von Ähnlichkeitsschwellenwerten und Ergebnisanzahl</li>
 *   <li>Fallback-Mechanismus: Bei leeren Suchergebnissen wird eine erneute Suche mit der Aufgabenstellungmarkdown durchgeführt</li>
 * </ul>
 * <p>
 * Der Fallback-Mechanismus sorgt dafür, dass auch bei spezifischen Nutzeranfragen, die keine direkten 
 * Treffer in den Kursmaterialien finden, allgemeine aufgabenbezogene Informationen bereitgestellt werden können.
 * <p>
 * Verwendung im RAG-Workflow:
 * <pre>
 * {@code
 * DocumentRetriever retriever = new VektorSpeicherDocumentRetriever(vektorSpeicherService)
 *     .withSimilarityThreshold(0.7f)
 *     .withTopK(3);
 *     
 * RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
 *     .documentRetriever(retriever)
 *     .queryAugmenter(queryAugmenter)
 *     .build();
 * }
 * </pre>
 *
 * @see de.fuh.kn.webapp.llm.rag.VektorSpeicherService
 * @see org.springframework.ai.rag.retrieval.search.DocumentRetriever
 * @see org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
 */
public class VektorSpeicherDocumentRetriever implements DocumentRetriever {

    /**
     * Der zu verwendende VektorSpeicherService für die Dokumentsuche.
     */
    private final VektorSpeicherService vektorSpeicherService;
    
    /**
     * Der Schwellenwert für die Ähnlichkeit von Dokumenten (0.0 - 1.0).
     * Dokumente mit einer niedrigeren Ähnlichkeit werden nicht zurückgegeben.
     */
    private float similarityThreshold = 0.65f;
    
    /**
     * Die maximale Anzahl zurückzugebender Dokumente.
     */
    private int topK = 5;

    /**
     * Erstellt einen neuen VektorSpeicherDocumentRetriever mit dem angegebenen VektorSpeicherService.
     *
     * @param vektorSpeicherService Der zu verwendende VektorSpeicherService für die Dokumentsuche
     */
    public VektorSpeicherDocumentRetriever(VektorSpeicherService vektorSpeicherService) {
        this.vektorSpeicherService = vektorSpeicherService;
    }

    /**
     * Führt eine hybride Dokumentsuche mit dem VektorSpeicherService durch.
     * <p>
     * Diese Methode:
     * <ol>
     *   <li>Extrahiert optional einen Filterausdruck aus dem Query-Kontext</li>
     *   <li>Erweitert die Suchanfrage mit der Aufgabenstellungmarkdown</li>
     *   <li>Ruft die hybride Suche des VektorSpeicherService auf</li>
     *   <li>Falls keine Ergebnisse gefunden werden, führt eine Fallback-Suche nur mit der Aufgabenstellungmarkdown durch</li>
     *   <li>Gibt die gefundenen Dokumente zurück</li>
     * </ol>
     * <p>
     * Die hybride Suche kombiniert:
     * <ul>
     *   <li>Vektorbasierte semantische Ähnlichkeitssuche</li>
     *   <li>Textbasierte TF-IDF Relevanzsuche</li>
     * </ul>
     * <p>
     * Der Fallback-Mechanismus stellt sicher, dass auch bei sehr spezifischen Anfragen, die keine 
     * direkten Treffer in den Kursmaterialien finden, allgemeine aufgabenbezogene Informationen 
     * aus der Aufgabenstellungmarkdown bereitgestellt werden können.
     *
     * @param query Die RAG Query, die den Suchtext und optionale Kontextparameter enthält
     * @return Eine Liste der relevantesten gefundenen Dokumente
     * @see de.fuh.kn.webapp.llm.rag.VektorSpeicherService#hybrideSuche(String, int, float, String, boolean)
     */
    @Override
    public List<Document> retrieve(Query query) {
        // Extrahiere den Filterausdruck aus dem Query-Kontext, falls vorhanden
        String filterExpression = null;
        if (query.context().containsKey("FILTER_EXPRESSION")) {
            filterExpression = (String) query.context().get("FILTER_EXPRESSION");
        }

        String searchQuery = query.text();
        
        // Erweitere die Suchanfrage mit Aufgabenkontext für bessere Ergebnisse
        TeilaufgabeDto teilaufgabe =(TeilaufgabeDto) query.context().get("TEILAUFGABE");
        if(teilaufgabe != null) {
            searchQuery += "\n\nAufgabe: " + teilaufgabe.getAufgabenstellungMarkdown();
        }
        
        // Extrahiere die kurseinheitId aus der Aufgabe für Relevanz-Boosting
        Long kurseinheitId = null;
        if (query.context().containsKey("AUFGABE")) {
            AufgabeDto aufgabe = (AufgabeDto) query.context().get("AUFGABE");
            kurseinheitId = aufgabe.getKurseinheitId();
        }

        // Primäre Suche durchführen
        List<Document> results = vektorSpeicherService.hybrideSuche(
            searchQuery,
            topK,
            similarityThreshold,
            filterExpression,
            true,  // Query-Transformation aktivieren
            kurseinheitId
        );
        
        // Fallback: Wenn keine Dokumente gefunden wurden, nutze aufgabenstellungmarkdown für eine erneute Suche
        if (results.isEmpty() && teilaufgabe != null && teilaufgabe.getAufgabenstellungMarkdown() != null 
            && !teilaufgabe.getAufgabenstellungMarkdown().trim().isEmpty()) {
            
            // Nutze nur die aufgabenstellungmarkdown für die Fallback-Suche
            String fallbackQuery = teilaufgabe.getAufgabenstellungMarkdown();
            
            results = vektorSpeicherService.hybrideSuche(
                fallbackQuery,
                topK,
                similarityThreshold,
                filterExpression,
                true,  // Query-Transformation aktivieren
                kurseinheitId
            );
        }

        return results;
    }

    /**
     * Setzt die Anzahl der maximal zurückzugebenden Dokumente.
     * <p>
     * Dieses Fluent-Interface ermöglicht die Verkettung von Konfigurationsmethoden.
     * 
     * @param topK Die maximale Anzahl zurückzugebender Dokumente (muss größer als 0 sein)
     * @return Diese Instanz für Method Chaining
     * @throws IllegalArgumentException wenn topK kleiner oder gleich 0 ist
     */
    public VektorSpeicherDocumentRetriever withTopK(int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK muss größer als 0 sein");
        }
        this.topK = topK;
        return this;
    }

    /**
     * Setzt den Schwellenwert für die Ähnlichkeit von Dokumenten.
     * <p>
     * Dieses Fluent-Interface ermöglicht die Verkettung von Konfigurationsmethoden.
     * 
     * @param similarityThreshold Der Schwellenwert zwischen 0.0 und 1.0
     * @return Diese Instanz für Method Chaining
     * @throws IllegalArgumentException wenn similarityThreshold außerhalb des gültigen Bereichs liegt
     */
    public VektorSpeicherDocumentRetriever withSimilarityThreshold(float similarityThreshold) {
        if (similarityThreshold < 0.0f || similarityThreshold > 1.0f) {
            throw new IllegalArgumentException("similarityThreshold muss zwischen 0.0 und 1.0 liegen");
        }
        this.similarityThreshold = similarityThreshold;
        return this;
    }
}