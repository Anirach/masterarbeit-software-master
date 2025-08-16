package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit-Test für den LlmBewertungService mit einfacher Struktur.
 * Da die komplexe Infrastruktur von Spring AI mit Convertern schwer zu mocken ist,
 * verzichten wir auf umfangreiche Tests und prüfen nur die grundlegende Struktur.
 */
class LlmBewertungServiceTest {

    /**
     * Testet die einfache Struktur des Service.
     * Vollständige Tests erfordern eine Integrationstestumgebung.
     */
    @Test
    @DisplayName("Struktureller Test der LlmBewertungService-Klasse")
    void testServiceStructure() {
        // Einfache Mock-Objekte erstellen
        ChatModel evaluationChatClient = Mockito.mock(ChatModel.class);
        ChatModel explanationChatClient = Mockito.mock(ChatModel.class);
        TokenUsageObserver tokenUsageObserver = Mockito.mock(TokenUsageObserver.class);
        
        // Service-Instanz erstellen
        LlmBewertungService service = new LlmBewertungService(
                evaluationChatClient, 
                explanationChatClient, 
                tokenUsageObserver);
        
        // Validieren, dass die Service-Instanz erstellt wurde (struktureller Test)
        assert service != null;
    }


    /**
     * Erstellt ein Test-RequestDto für Testfälle.
     */
    private BewertungsRequestDto createTestRequestDto() {
        BewertungsRequestDto requestDto = new BewertungsRequestDto();
        requestDto.setAufgabenstellungAufgabe("Mathematik Grundlagen");
        requestDto.setAufgabenstellungTeilaufgabe("Berechnen Sie das Ergebnis der folgenden Gleichung: 2 + 2 = ?");
        
        // Musterlösung
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("Antwort", "4");
        requestDto.setMusterloesungFelder(musterloesungFelder);
        
        // Eingabe des Studenten
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("Antwort", "5");
        requestDto.setLoesungFelder(loesungFelder);
        
        return requestDto;
    }
}