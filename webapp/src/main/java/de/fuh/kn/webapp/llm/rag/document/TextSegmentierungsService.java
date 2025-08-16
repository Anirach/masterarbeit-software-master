package de.fuh.kn.webapp.llm.rag.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service für die Segmentierung von Text in überlappende Chunks für die Vektorsuche.
 * Verwendet Spring AI's TextSplitter-Implementierungen und eine eigene OverlappingTokenTextSplitter-Implementierung
 * für effiziente Chunking-Strategien mit kontrollierter Überlappung.
 */
@Service
@Slf4j
public class TextSegmentierungsService {
    
    /**
     * Standardgröße eines Chunks in Tokens.
     */
    @Value("${app.ai.rag.chunk-size:500}")
    private int standardChunkGroesse;
    
    /**
     * Überlappung zwischen benachbarten Chunks in Prozent.
     */
    @Value("${app.ai.rag.chunk-overlap:20}")
    private int standardUeberlappungProzent;
    
    /**
     * Segmentiert das übergebene Document in überlappende Chunks mit Standardeinstellungen.
     * Verwendet die semantische Segmentierungsstrategie (ParagraphTextSplitter).
     *
     * @param document Das Spring AI Document mit dem zu segmentierenden Text.
     * @return Eine Liste von Document-Objekten mit den segmentierten Chunks.
     */
    public List<Document> segmentiereText(Document document) {
        return segmentiereText(document, standardChunkGroesse, standardUeberlappungProzent);
    }
    
    /**
     * Segmentiert das übergebene Document in überlappende Chunks mit benutzerdefinierten Einstellungen.
     *
     * @param document Das Spring AI Document mit dem zu segmentierenden Text.
     * @param chunkGroesse Die gewünschte Chunk-Größe in Tokens.
     * @param ueberlappungProzent Der Überlappungsprozentsatz zwischen benachbarten Chunks.
     * @return Eine Liste von Document-Objekten mit den segmentierten Chunks.
     */
    public List<Document> segmentiereText(
            Document document,
            int chunkGroesse,
            int ueberlappungProzent) {
        
        if (document == null || document.getText() == null || document.getText().isEmpty()) {
            log.warn("Dokument ist leer oder null");
            return List.of();
        }
        
        // Überlappung in Tokens berechnen
        int ueberlappung = (chunkGroesse * ueberlappungProzent) / 100;
        
        // TextSplitter auswählen je nach Strategie
        TextSplitter splitter = OverlappingTokenTextSplitter.builder()
                    .withChunkSize(chunkGroesse)
                    .withChunkOverlap(ueberlappung)
                    .withMinChunkSizeChars((int)(chunkGroesse * 0.6))
                    .withMinChunkLengthToEmbed(10)
                    .withMaxNumChunks(5000)
                    .withKeepSeparator(true)
                    .build();

        try {
            // Dokument segmentieren
            List<Document> segments = splitter.apply(List.of(document));
            
            log.info("Dokument in {} Segmente aufgeteilt (Größe: {} Tokens, Überlappung: {} Tokens)",
                    segments.size(), chunkGroesse, ueberlappung);
            
            return segments;
        } catch (Exception e) {
            log.error("Fehler bei der Textsegmentierung: {}", e.getMessage(), e);
            
            // Fallback zur einfachsten Methode
            log.info("Verwende Fallback-Segmentierung (Token-basiert mit Standardeinstellungen)");
            return new TokenTextSplitter().apply(List.of(document));
        }
    }
    
    /**
     * Aufzählung der verfügbaren Segmentierungsstrategien.
     * Jede Strategie verwendet den OverlappingTokenTextSplitter mit unterschiedlichen Konfigurationen.
     */
    public enum SegmentierungsStrategie {
        /**
         * Grobgranulare token-basierte Segmentierung.
         * Verwendet größere minChunkSizeChars und minChunkLengthToEmbed-Werte,
         * um weniger, aber längere zusammenhängende Abschnitte zu bevorzugen.
         * Geeignet für gut strukturierte Dokumente, bei denen Kontext erhalten bleiben soll.
         */
        PARAGRAF
    }
}