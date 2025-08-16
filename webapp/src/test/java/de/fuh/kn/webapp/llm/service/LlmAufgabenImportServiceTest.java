package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfImportRequestDto;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfPairImportRequestDto;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmAufgabenImportServiceTest {

    @Mock
    private ChatModel pdfImportChatModel;
    
    @Mock
    private TokenUsageObserver tokenUsageObserver;
    
    @InjectMocks
    private LlmAufgabenImportService llmAufgabenImportService;
    
    private LlmPdfImportRequestDto singlePdfRequest;
    private LlmPdfPairImportRequestDto pdfPairRequest;
    private Resource testResource;
    
    @BeforeEach
    void setUp() {
        testResource = new ByteArrayResource("Test PDF content".getBytes());
        
        singlePdfRequest = new LlmPdfImportRequestDto(testResource, "application/pdf");
        
        pdfPairRequest = new LlmPdfPairImportRequestDto(
            testResource, "application/pdf",
            testResource, "application/pdf"
        );
    }
    
    @Test
    @DisplayName("extractAufgabenFromPdf sollte RuntimeException werfen bei LLM-Fehler")
    void testExtractAufgabenFromPdf_ShouldThrowRuntimeException_WhenLlmFails() {
        // Arrange
        when(pdfImportChatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
            .thenThrow(new RuntimeException("LLM Service Error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> llmAufgabenImportService.extractAufgabenFromPdf(singlePdfRequest)
        );
        
        assertTrue(exception.getMessage().contains("Fehler bei der Verarbeitung der PDF-Datei"));
        assertTrue(exception.getMessage().contains("LLM Service Error"));
        
        verify(pdfImportChatModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    }
    
    @Test
    @DisplayName("extractAufgabenFromPdfPair sollte RuntimeException werfen bei LLM-Fehler")
    void testExtractAufgabenFromPdfPair_ShouldThrowRuntimeException_WhenLlmFails() {
        // Arrange
        when(pdfImportChatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
            .thenThrow(new RuntimeException("PDF Processing Error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> llmAufgabenImportService.extractAufgabenFromPdfPair(pdfPairRequest)
        );
        
        assertTrue(exception.getMessage().contains("Fehler bei der Verarbeitung der PDF-Dateien"));
        assertTrue(exception.getMessage().contains("PDF Processing Error"));
        
        verify(pdfImportChatModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    }
    
    @Test
    @DisplayName("LlmPdfImportRequestDto sollte korrekte Werte haben")
    void testLlmPdfImportRequestDto_ShouldHaveCorrectValues() {
        // Assert
        assertEquals(testResource, singlePdfRequest.getResource());
        assertEquals("application/pdf", singlePdfRequest.getContentType());
    }
    
    @Test
    @DisplayName("LlmPdfPairImportRequestDto sollte korrekte Werte haben")
    void testLlmPdfPairImportRequestDto_ShouldHaveCorrectValues() {
        // Assert
        assertEquals(testResource, pdfPairRequest.getAssignmentResource());
        assertEquals("application/pdf", pdfPairRequest.getAssignmentContentType());
        assertEquals(testResource, pdfPairRequest.getSolutionResource());
        assertEquals("application/pdf", pdfPairRequest.getSolutionContentType());
    }
    
    @Test
    @DisplayName("extractAufgabenFromPdf sollte bei null Resource fehlschlagen")
    void testExtractAufgabenFromPdf_ShouldFail_WhenResourceIsNull() {
        // Arrange
        LlmPdfImportRequestDto requestWithNullResource = new LlmPdfImportRequestDto(null, "application/pdf");
        
        // Act & Assert
        assertThrows(
            Exception.class,
            () -> llmAufgabenImportService.extractAufgabenFromPdf(requestWithNullResource)
        );
    }
    
    @Test
    @DisplayName("extractAufgabenFromPdfPair sollte bei null Assignment Resource fehlschlagen")
    void testExtractAufgabenFromPdfPair_ShouldFail_WhenAssignmentResourceIsNull() {
        // Arrange
        LlmPdfPairImportRequestDto requestWithNullAssignment = new LlmPdfPairImportRequestDto(
            null, "application/pdf",
            testResource, "application/pdf"
        );
        
        // Act & Assert
        assertThrows(
            Exception.class,
            () -> llmAufgabenImportService.extractAufgabenFromPdfPair(requestWithNullAssignment)
        );
    }
}