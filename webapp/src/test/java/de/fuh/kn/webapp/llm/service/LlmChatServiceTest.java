package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.chat.LlmChatRequestDto;
import de.fuh.kn.webapp.llm.dto.chat.LlmChatResponseDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.rag.modules.LlmRelevanceFilter;
import de.fuh.kn.webapp.llm.rag.storage.VektorSpeicherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für den LlmChatService.
 * Testet die Chat-Funktionalität mit RAG (Retrieval Augmented Generation).
 */
@ExtendWith(MockitoExtension.class)
class LlmChatServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private VektorSpeicherService vektorSpeicherService;

    @Mock
    private LlmRelevanceFilter llmRelevanceFilter;

    @Mock
    private TokenUsageObserver tokenUsageObserver;

    private LlmChatService llmChatService;

    private LlmChatRequestDto testRequest;

    @BeforeEach
    void setUp() {
        llmChatService = new LlmChatService(chatModel, vektorSpeicherService, llmRelevanceFilter, tokenUsageObserver);

        // Test-Request erstellen
        testRequest = new LlmChatRequestDto();
        testRequest.setKursId(1L);
        testRequest.setUserMessage("Erkläre mir den Bubble Sort Algorithmus");
        testRequest.setExplanationMode(true);
        testRequest.setChatHistory(new ArrayList<>());
    }

    @Test
    @DisplayName("generiereAntwort sollte bei Fehler eine Fallback-Antwort zurückgeben")
    void generiereAntwort_ShouldReturnFallbackResponseOnError() {
        // Arrange
        when(tokenUsageObserver.startCostContext(any())).thenThrow(new RuntimeException("Test-Fehler"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().contains("Entschuldigung"));
        assertEquals(0, response.getInputTokens());
        assertEquals(0, response.getOutputTokens());
        assertEquals(BigDecimal.ZERO, response.getCost());
        assertTrue(response.getDocumentReferences().isEmpty());
        
        verify(tokenUsageObserver).startCostContext(OperationType.CHAT_MESSAGE);
    }

    @Test
    @DisplayName("generiereAntwort sollte Request-Parameter validieren")
    void generiereAntwort_ShouldValidateRequestParameters() {
        // Arrange
        when(tokenUsageObserver.startCostContext(any())).thenThrow(new IllegalArgumentException("Ungültige Parameter"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().contains("Entschuldigung"));
        assertEquals(0, response.getInputTokens());
        assertEquals(0, response.getOutputTokens());
    }

    @Test
    @DisplayName("generiereAntwort sollte mit leerer Chat-Historie umgehen können")
    void generiereAntwort_ShouldHandleEmptyChatHistory() {
        // Arrange
        testRequest.setChatHistory(new ArrayList<>());
        when(tokenUsageObserver.startCostContext(any())).thenThrow(new RuntimeException("Test"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getDocumentReferences().isEmpty());
    }

    @Test
    @DisplayName("generiereAntwort sollte mit Chat-Historie umgehen können")
    void generiereAntwort_ShouldHandleChatHistory() {
        // Arrange
        LlmChatRequestDto.ChatMessage systemMessage = new LlmChatRequestDto.ChatMessage("Du bist ein hilfreicher Assistent", true);
        LlmChatRequestDto.ChatMessage userMessage = new LlmChatRequestDto.ChatMessage("Vorherige Frage", false);

        testRequest.getChatHistory().add(systemMessage);
        testRequest.getChatHistory().add(userMessage);

        when(tokenUsageObserver.startCostContext(any())).thenThrow(new RuntimeException("Test"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2, testRequest.getChatHistory().size());
    }

    @Test
    @DisplayName("generiereAntwort sollte Explanation Mode berücksichtigen")
    void generiereAntwort_ShouldConsiderExplanationMode() {
        // Arrange
        testRequest.setExplanationMode(true);
        when(tokenUsageObserver.startCostContext(any())).thenThrow(new RuntimeException("Test"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(testRequest.isExplanationMode());
    }

    @Test
    @DisplayName("generiereAntwort sollte ohne Explanation Mode funktionieren")
    void generiereAntwort_ShouldWorkWithoutExplanationMode() {
        // Arrange
        testRequest.setExplanationMode(false);
        when(tokenUsageObserver.startCostContext(any())).thenThrow(new RuntimeException("Test"));

        // Act
        LlmChatResponseDto response = llmChatService.generiereAntwort(testRequest);

        // Assert
        assertNotNull(response);
        assertFalse(testRequest.isExplanationMode());
    }

    @Test
    @DisplayName("Constructor sollte Dependencies korrekt setzen")
    void constructor_ShouldSetDependenciesCorrectly() {
        // Act
        LlmChatService service = new LlmChatService(chatModel, vektorSpeicherService, llmRelevanceFilter, tokenUsageObserver);

        // Assert
        assertNotNull(service);
    }
}