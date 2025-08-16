package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.llm.dto.bewertung.FeedbackResponseDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integrationstest für den LlmBewertungService.
 * Verwendet tatsächliche API-Aufrufe, daher mit @Tag("integration") markiert.
 * 
 * HINWEIS: Dieser Test verursacht tatsächliche API-Kosten. 
 * Er verwendet bewusst das günstigste Modell (gpt-4.1-nano) durch das "test"-Profil,
 * sollte aber trotzdem sparsam eingesetzt werden.
 * 
 * Die Verwendung des Nano-Modells ist im application-test.yml konfiguriert, welches alle Anfragen 
 * mit diesem günstigsten Modell ausführt.
 */
@SpringBootTest
@ActiveProfiles({"test", "local"})
@Tag("integration")
class LlmBewertungServiceIntegrationTest {

    @Autowired
    private LlmBewertungService llmBewertungService;
    
    @Autowired
    private TokenUsageObserver tokenUsageObserver;

    private BewertungsRequestDto requestDto;
    private static final Long TEST_STUDENT_ID = 1001L;

    @BeforeEach
    void setUp() {
        // Einfache Testdaten mit einer mathematischen Aufgabe
        requestDto = new BewertungsRequestDto();
        requestDto.setAufgabenstellungAufgabe("Mathematik Grundlagen");
        requestDto.setAufgabenstellungTeilaufgabe("Berechnen Sie das Ergebnis der folgenden Gleichung: 2 + 2 = ?");
        requestDto.setStudentId(TEST_STUDENT_ID); // Für Kostenzuordnung
        
        // Musterlösung
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("Antwort", "4");
        requestDto.setMusterloesungFelder(musterloesungFelder);
        
        // Eingabe des Studenten
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("Antwort", "5");
        requestDto.setLoesungFelder(loesungFelder);
    }

    /**
     * Testet die grundlegende Funktionalität der Bewertung mit dem erweiterten Token-Tracking.
     * Verwendet tatsächliche API-Aufrufe zum GPT-4.1-nano Modell.
     */
    @Test
    @DisplayName("LLM-Bewertung mit Token-Tracking und Kostenzuordnung")
    void testEvaluateSolution_WithTokenTracking() {
        // Start cost tracking
        String contextId = tokenUsageObserver.startCostContext(OperationType.EVALUATION);
        
        // Act - API-Aufruf durchführen
        BewertungsResponseDto responseDto = llmBewertungService.evaluateSolution(requestDto);
        
        // End cost tracking 
        TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
        
        // Assert - Überprüfen, dass eine Antwort zurückgegeben wurde
        assertNotNull(responseDto);
        assertNotNull(responseDto.getPunkte());
        assertNotNull(responseDto.getFeedback());
        assertNotNull(responseDto.getFelderBewertung());
        assertNotNull(responseDto.getInputToken());
        assertNotNull(responseDto.getOutputToken());
        assertNotNull(responseDto.getCost());
        
        // Die Antwort ist falsch, sollte also nicht die volle Punktzahl bekommen
        assertTrue(responseDto.getPunkte() < 100);
        
        // Die Bewertung der Felder sollte die Eingabefelder enthalten
        assertTrue(responseDto.getFelderBewertung().containsKey("Antwort"));
        
        // Verify cost tracking
        assertEquals(OperationType.EVALUATION, costContext.getOperationType());
        assertNull(costContext.getUserId());
        assertEquals(responseDto.getInputToken(), costContext.getInputTokens());
        assertEquals(responseDto.getOutputToken(), costContext.getOutputTokens());
        assertEquals(0, responseDto.getCost().compareTo(costContext.getTotalCost()), 
                     "CostContext und ResponseDto sollten identische Kosten haben");
        
        // Verify model information is captured
        assertNotNull(costContext.getModel(), "Das Modell sollte erfasst sein");
        assertTrue(costContext.getModel().contains("gpt-"), "Das Modell sollte von OpenAI sein");
    }

    /**
     * Testet die grundlegende Funktionalität der Feedback-Generierung mit dem erweiterten Token-Tracking.
     * Verwendet tatsächliche API-Aufrufe zum GPT-4.1-nano Modell.
     */
    @Test
    @DisplayName("LLM-Feedbackgenerierung mit Token-Tracking und Kostenzuordnung")
    @Disabled("Zu Fehleranfällig")
    void testGenerateFeedback_WithTokenTracking() {
        // Start cost tracking
        String contextId = tokenUsageObserver.startCostContext(OperationType.FEEDBACK);
        
        // Act - API-Aufruf durchführen
        FeedbackResponseDto response = llmBewertungService.generateFeedback(requestDto, 50);
        
        // End cost tracking
        TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
        
        // Assert - Überprüfen, dass eine Antwort zurückgegeben wurde
        assertNotNull(response);
        assertNotNull(response.getFeedback());
        assertTrue(response.getFeedback().length() > 0);
        assertEquals(50, response.getScore());
        assertNotNull(response.getInputToken());
        assertNotNull(response.getOutputToken());
        assertNotNull(response.getCost());
        
        // Das Feedback sollte relevante Informationen zur Aufgabe enthalten
        assertTrue(response.getFeedback().toLowerCase().contains("gleichung") || 
                   response.getFeedback().toLowerCase().contains("berechnung") || 
                   response.getFeedback().toLowerCase().contains("richtige antwort") ||
                   response.getFeedback().toLowerCase().contains("lösung"),
                  "Feedback sollte relevante Begriffe zur Aufgabe enthalten");
        
        // Verify cost tracking
        assertEquals(OperationType.FEEDBACK, costContext.getOperationType());
        assertNull(costContext.getUserId());
        assertEquals(response.getInputToken(), costContext.getInputTokens());
        assertEquals(response.getOutputToken(), costContext.getOutputTokens());
        assertEquals(0, response.getCost().compareTo(costContext.getTotalCost()), 
                     "CostContext und ResponseDto sollten identische Kosten haben");
    }
    
    /**
     * Testet eine Anfrage mit minimalen Informationen mit dem erweiterten Token-Tracking.
     * Verwendet tatsächliche API-Aufrufe zum GPT-4.1-nano Modell.
     */
    @Test
    @DisplayName("LLM-Bewertung mit minimalen Informationen und Token-Tracking")
    void testEvaluateSolution_MinimalInfoWithTokenTracking() {
        // Arrange
        BewertungsRequestDto minimalRequestDto = new BewertungsRequestDto();
        minimalRequestDto.setAufgabenstellungAufgabe("Einfache Frage");
        minimalRequestDto.setAufgabenstellungTeilaufgabe("Was ist 1+1?");
        minimalRequestDto.setStudentId(TEST_STUDENT_ID);
        
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("Antwort", "2");
        minimalRequestDto.setMusterloesungFelder(musterloesungFelder);
        
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("Antwort", "2");
        minimalRequestDto.setLoesungFelder(loesungFelder);
        
        // Start cost tracking
        String contextId = tokenUsageObserver.startCostContext(OperationType.EVALUATION);
        
        // Act
        BewertungsResponseDto responseDto = llmBewertungService.evaluateSolution(minimalRequestDto);
        
        // End cost tracking
        TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
        
        // Assert
        assertNotNull(responseDto);
        assertNotNull(responseDto.getPunkte());
        assertNotNull(responseDto.getInputToken());
        assertNotNull(responseDto.getOutputToken());
        assertNotNull(responseDto.getCost());
        
        // Die Antwort ist korrekt, sollte also die volle Punktzahl bekommen
        assertEquals(100, responseDto.getPunkte(), 
                    "Korrekte Antwort sollte 100 Punkte erhalten");
        
        // Verify cost tracking
        assertEquals(OperationType.EVALUATION, costContext.getOperationType());
        assertNull(costContext.getUserId());
        assertEquals(responseDto.getInputToken(), costContext.getInputTokens());
        assertEquals(responseDto.getOutputToken(), costContext.getOutputTokens());
        assertEquals(0, responseDto.getCost().compareTo(costContext.getTotalCost()),
                     "CostContext und ResponseDto sollten identische Kosten haben");
    }
    
    /**
     * Testet verschachtelte Kontexte für Token-Tracking.
     */
    @Test
    @DisplayName("Test verschachtelter Token-Tracking-Kontexte")
    void testNestedTokenTrackingContexts() {
        // Arrange
        BewertungsRequestDto minimalRequestDto = new BewertungsRequestDto();
        minimalRequestDto.setAufgabenstellungAufgabe("Einfache Frage");
        minimalRequestDto.setAufgabenstellungTeilaufgabe("Was ist 1+1?");
        minimalRequestDto.setStudentId(TEST_STUDENT_ID);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("Antwort", "2");
        minimalRequestDto.setMusterloesungFelder(fields);
        minimalRequestDto.setLoesungFelder(fields);
        
        // Start outer cost tracking context
        String outerContextId = tokenUsageObserver.startCostContext(OperationType.EVALUATION);
        
        // Start inner cost tracking context - should reuse outer context
        String innerContextId = tokenUsageObserver.startCostContext(OperationType.EXPLANATION);
        
        // Verify both contexts have same ID (outer context prevails)
        assertEquals(outerContextId, innerContextId);
        
        // Act
        BewertungsResponseDto responseDto = llmBewertungService.evaluateSolution(minimalRequestDto);
        
        // End inner context - should not actually end
        TokenUsageObserver.CostContext innerContext = tokenUsageObserver.endCostContext();
        
        // End outer context - should fully end
        TokenUsageObserver.CostContext outerContext = tokenUsageObserver.endCostContext();
        
        // Verify inner and outer contexts are the same object
        assertSame(innerContext, outerContext);
        
        // Both should have the original operation type
        assertEquals(OperationType.EVALUATION, outerContext.getOperationType());
        assertNull(outerContext.getUserId());
        
        // Verify token counts and costs
        assertEquals(responseDto.getInputToken(), outerContext.getInputTokens());
        assertEquals(responseDto.getOutputToken(), outerContext.getOutputTokens());
        assertEquals(0, responseDto.getCost().compareTo(outerContext.getTotalCost()));
    }
    
    /**
     * Testet die Bewertung einer komplexeren Programmierlösung mit Token-Tracking.
     * Da dieser Test mehr Token verbraucht und etwas länger dauert,
     * wird er nur manuell ausgeführt.
     */
    @Test
    @DisplayName("Test der LLM-Bewertung einer Programmieraufgabe mit Token-Tracking")
    void testEvaluateSolution_ProgrammingTaskWithTokenTracking() {
        // Test nur manuell ausführen, um Tokens zu sparen
        assumeTrue(Boolean.getBoolean("runComplexTests"), 
                  "Überspringe komplexen Test zur Token-Einsparung");
        
        // Arrange
        BewertungsRequestDto programRequestDto = new BewertungsRequestDto();
        programRequestDto.setAufgabenstellungAufgabe("Java Programmierung");
        programRequestDto.setAufgabenstellungTeilaufgabe(
            "Implementieren Sie eine Methode, die prüft ob ein String ein Palindrom ist.\n" +
            "Ein Palindrom ist ein Wort, das vorwärts und rückwärts gelesen gleich ist, " +
            "z.B. 'Anna' oder 'Reliefpfeiler'."
        );
        programRequestDto.setStudentId(TEST_STUDENT_ID);
        
        // Musterlösung
        Map<String, String> musterloesungFelder = new HashMap<>();
        musterloesungFelder.put("Code", 
            "public boolean isPalindrome(String str) {\n" +
            "    if (str == null) return false;\n" +
            "    String processed = str.toLowerCase().replaceAll(\"[^a-z0-9]\", \"\");\n" +
            "    int left = 0;\n" +
            "    int right = processed.length() - 1;\n" +
            "    while (left < right) {\n" +
            "        if (processed.charAt(left) != processed.charAt(right)) {\n" +
            "            return false;\n" +
            "        }\n" +
            "        left++;\n" +
            "        right--;\n" +
            "    }\n" +
            "    return true;\n" +
            "}"
        );
        programRequestDto.setMusterloesungFelder(musterloesungFelder);
        
        // Eine korrekte, aber anders formulierte Studentenlösung
        Map<String, String> loesungFelder = new HashMap<>();
        loesungFelder.put("Code", 
            "public boolean isPalindrome(String input) {\n" +
            "    if (input == null) return false;\n" +
            "    \n" +
            "    // String normalisieren: Kleinbuchstaben und nur alphanumerische Zeichen\n" +
            "    String str = input.toLowerCase().replaceAll(\"[^a-z0-9]\", \"\");\n" +
            "    \n" +
            "    // Umkehrung des normalisierten Strings erstellen\n" +
            "    StringBuilder reversed = new StringBuilder(str).reverse();\n" +
            "    \n" +
            "    // Prüfen, ob der normalisierte String und seine Umkehrung gleich sind\n" +
            "    return str.equals(reversed.toString());\n" +
            "}"
        );
        programRequestDto.setLoesungFelder(loesungFelder);
        
        // Bewertungshinweise
        programRequestDto.setBewertungshinweise(
            "Der Code sollte auf Null-Werte prüfen, Sonderzeichen entfernen und " +
            "Groß-/Kleinschreibung ignorieren."
        );
        
        // Start cost tracking
        String contextId = tokenUsageObserver.startCostContext(OperationType.EVALUATION);
        
        // Act
        BewertungsResponseDto responseDto = llmBewertungService.evaluateSolution(programRequestDto);
        
        // End cost tracking
        TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
        
        // Assert
        assertNotNull(responseDto);
        assertNotNull(responseDto.getPunkte());
        assertNotNull(responseDto.getFeedback());
        assertNotNull(responseDto.getFelderBewertung());
        assertNotNull(responseDto.getInputToken());
        assertNotNull(responseDto.getOutputToken());
        assertNotNull(responseDto.getCost());
        
        // Die Lösung ist korrekt, sollte also eine hohe Punktzahl bekommen
        assertTrue(responseDto.getPunkte() >= 90, 
                  "Korrekte alternative Implementierung sollte mindestens 90 Punkte erhalten");
        
        // Tokens und Kosten sollten signifikant sein
        assertTrue(responseDto.getInputToken() > 300, 
                  "Komplexe Aufgabe sollte signifikante Eingabe-Tokens haben");
        assertTrue(responseDto.getOutputToken() > 0,
                  "Output-Tokens sollten größer als 0 sein");
        assertTrue(responseDto.getCost().compareTo(BigDecimal.ZERO) > 0, 
                  "Kosten sollten größer als 0 sein");
        
        // Verify cost tracking
        assertEquals(OperationType.EVALUATION, costContext.getOperationType());
        assertNull(costContext.getUserId());
        assertEquals(responseDto.getInputToken(), costContext.getInputTokens());
        assertEquals(responseDto.getOutputToken(), costContext.getOutputTokens());
        assertEquals(0, responseDto.getCost().compareTo(costContext.getTotalCost()));
    }
}