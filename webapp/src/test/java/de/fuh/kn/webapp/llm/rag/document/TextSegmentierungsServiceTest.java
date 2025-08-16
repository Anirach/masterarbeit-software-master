package de.fuh.kn.webapp.llm.rag.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests für den TextSegmentierungsService zur Überprüfung der korrekten Einbindung
 * und Funktionalität der Textsegmentierung für RAG.
 */
class TextSegmentierungsServiceTest {
    
    private TextSegmentierungsService segmentierungsService;
    
    @BeforeEach
    void setup() {
        segmentierungsService = new TextSegmentierungsService();
        // Setzen der Properties manuell (statt TestPropertySource)
        ReflectionTestUtils.setField(segmentierungsService, "standardChunkGroesse", 300);
        ReflectionTestUtils.setField(segmentierungsService, "standardUeberlappungProzent", 25);
    }
    
    @Test
    @DisplayName("Sollte Text mit Standardeinstellungen segmentieren")
    void sollteTextMitStandardeinstellungenSegmentieren() {
        // Arrange
        String text = """
                Dies ist ein Beispieltext für den TextSegmentierungsService.
                Dieser Text sollte in mehrere Chunks aufgeteilt werden.
                
                Der TextSegmentierungsService verwendet eine verbesserte Implementierung,
                die eine präzise Kontrolle über die Token-Überlappung zwischen Chunks ermöglicht.
                
                Wir testen hier die Standardstrategie, die für die meisten Texttypen geeignet ist.
                Bei dieser Strategie werden ausgewogene Parameter verwendet.
                
                Die Segmentierung berücksichtigt token-basierte Grenzen und überlappende Bereiche,
                um eine optimale Retrievalleistung in der RAG-Pipeline zu gewährleisten.
                
                Mit der richtigen Segmentierungsstrategie kann das Retrieval relevanter Informationen
                deutlich verbessert werden, insbesondere wenn Kontext über Chunkgrenzen hinweg wichtig ist.
                """;
        
        Document document = new Document(text, Map.of("source", "test"));
        
        // Act
        List<Document> chunks = segmentierungsService.segmentiereText(document);
        
        // Assert
        assertThat(chunks).isNotEmpty();
        
        // Metadaten sollten erhalten bleiben
        for (Document chunk : chunks) {
            assertThat(chunk.getMetadata()).containsEntry("source", "test");
        }
    }
    
    @Test
    @DisplayName("Sollte verschiedene Segmentierungsstrategien korrekt anwenden")
    void sollteVerschiedeneStrategienKorrektAnwenden() {
        // Arrange
        String text = """
                Dies ist ein Text, der mit verschiedenen Strategien segmentiert werden soll.
                Wir testen hier verschiedene Konfigurationen der Textsegmentierung.
                
                Bei der PARAGRAF-Strategie werden größere zusammenhängende Chunks bevorzugt.
                Diese Strategie ist optimal für strukturierte Dokumente wie technische Dokumentation.
                """;
        
        Document document = new Document(text, Map.of("strategy", "test"));
        
        int chunkGroesse = 200;
        int ueberlappungProzent = 30;
        
        // Act
        List<Document> paragrafChunks = segmentierungsService.segmentiereText(
                document, chunkGroesse, ueberlappungProzent);
        
        // Assert
        assertThat(paragrafChunks).isNotEmpty();
    }
    
    @Test
    @DisplayName("Sollte leere Dokumente korrekt behandeln")
    void sollteLeeresDokumentKorrektBehandeln() {
        // Arrange
        Document emptyDocument = new Document("", Map.of());
        
        // Act
        List<Document> result = segmentierungsService.segmentiereText(emptyDocument);
        
        // Assert
        assertThat(result).isEmpty();
    }

}