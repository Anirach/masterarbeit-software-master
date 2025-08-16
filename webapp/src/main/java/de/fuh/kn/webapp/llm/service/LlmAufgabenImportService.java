package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfImportRequestDto;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfImportResponseDto;
import de.fuh.kn.webapp.llm.dto.pdfimport.LlmPdfPairImportRequestDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import de.fuh.kn.webapp.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service für die LLM-basierte Extraktion von Aufgaben aus PDF-Dateien.
 * Implementiert die Kernlogik für die Verarbeitung von PDF-Dateien
 * über Spring AI zur strukturierten Extraktion von Aufgaben und Lösungen.
 */
@Service
@Slf4j
public class LlmAufgabenImportService {

    private final ChatModel pdfImportChatModel;
    private final TokenUsageObserver tokenUsageObserver;

    public LlmAufgabenImportService(@Qualifier("pdfImportChatClient") ChatModel pdfImportChatModel, TokenUsageObserver tokenUsageObserver) {
        this.pdfImportChatModel = pdfImportChatModel;
        this.tokenUsageObserver = tokenUsageObserver;
    }

    /**
     * Extrahiert Aufgaben aus einer einzelnen PDF-Datei über LLM.
     *
     * @param request Die Anfrage mit der PDF-Datei und Kontext-Informationen
     * @return Ein DTO mit den extrahierten Aufgaben und Metadaten
     */
    public LlmPdfImportResponseDto extractAufgabenFromPdf(LlmPdfImportRequestDto request) {
        try {
            // Kostentracking starten mit Operationstyp
            tokenUsageObserver.startCostContext(OperationType.PDF_IMPORT);

            // Converter für die strukturierte Ausgabe konfigurieren
            BeanOutputConverter<LlmPdfImportResponseDto> converter = new BeanOutputConverter<>(LlmPdfImportResponseDto.class);

            // PDF-Datei als Anhang hinzufügen
            Media media = new Media(MimeType.valueOf(request.getContentType()), request.getResource());

            // Prompt mit Template und Parametern erstellen
            UserMessage userMessage = UserMessage.builder()
                    .text(PromptTemplates.PDF_IMPORT_TEMPLATE)
                    .media(media)
                    .build();

            Prompt prompt = new Prompt(userMessage);

            // LLM aufrufen
            ChatResponse response = pdfImportChatModel.call(prompt);
            String responseText = response.getResult().getOutput().getText();
            
            log.debug("Erhaltene Antwort vom LLM: {}", responseText);
            
            // Backslashes in LaTeX-Ausdrücken für JSON escapen
            String sanitizedResponseText = JsonUtil.sanitizeJsonResponse(responseText);
            
            // Strukturierte Ausgabe konvertieren
            LlmPdfImportResponseDto result = converter.convert(sanitizedResponseText);
            
            // Tokens erfassen
            Usage usage = response.getMetadata().getUsage();
            result.setInputToken(usage.getPromptTokens());
            result.setOutputToken(usage.getCompletionTokens());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            BigDecimal totalCost = costContext.getTotalCost();
            log.info("Aufgabenextraktion aus PDF durchgeführt mit {} Input-Token und {} Output-Token. Kosten: ${}", 
                     costContext.getInputTokens(),
                     costContext.getOutputTokens(),
                     totalCost.toPlainString());
            
            // Modell erfassen
            result.setModel(costContext.getModel());
            
            // Kosten in die Antwort setzen
            result.setCost(totalCost);
            
            return result;
        } catch (Exception e) {
            log.error("Fehler bei der LLM-basierten Extraktion von Aufgaben aus PDF-Datei", e);
            throw new RuntimeException("Fehler bei der Verarbeitung der PDF-Datei: " + e.getMessage(), e);
        }
    }

    /**
     * Extrahiert Aufgaben aus zwei PDF-Dateien (Aufgaben und Lösungen) über LLM.
     *
     * @param request Die Anfrage mit beiden PDF-Dateien und Kontext-Informationen
     * @return Ein DTO mit den extrahierten Aufgaben und Metadaten
     */
    public LlmPdfImportResponseDto extractAufgabenFromPdfPair(LlmPdfPairImportRequestDto request) {
        try {
            // Kostentracking starten mit Operationstyp
            tokenUsageObserver.startCostContext(OperationType.PDF_IMPORT);

            // Converter für die strukturierte Ausgabe konfigurieren
            BeanOutputConverter<LlmPdfImportResponseDto> converter = new BeanOutputConverter<>(LlmPdfImportResponseDto.class);

            // PDF-Dateien als Anhänge hinzufügen
            Media assignmentMedia = new Media(MimeType.valueOf(request.getAssignmentContentType()), request.getAssignmentResource());
            Media solutionMedia = new Media(MimeType.valueOf(request.getSolutionContentType()), request.getSolutionResource());

            // Prompt mit Template und Parametern erstellen
            UserMessage userMessage = UserMessage.builder()
                    .text(PromptTemplates.PDF_PAIR_IMPORT_TEMPLATE)
                    .media(List.of(assignmentMedia, solutionMedia))
                    .build();

            Prompt prompt = new Prompt(userMessage);

            // LLM aufrufen
            ChatResponse response = pdfImportChatModel.call(prompt);
            String responseText = response.getResult().getOutput().getText();

            log.debug("Erhaltene Antwort vom LLM: {}", responseText);
            
            // Backslashes in LaTeX-Ausdrücken für JSON escapen
            String sanitizedResponseText = JsonUtil.sanitizeJsonResponse(responseText);
            
            // Strukturierte Ausgabe konvertieren
            LlmPdfImportResponseDto result = converter.convert(sanitizedResponseText);
            
            // Tokens erfassen
            Usage usage = response.getMetadata().getUsage();
            result.setInputToken(usage.getPromptTokens());
            result.setOutputToken(usage.getCompletionTokens());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            BigDecimal totalCost = costContext.getTotalCost();
            log.info("Aufgabenextraktion aus PDF-Paar durchgeführt mit {} Input-Token und {} Output-Token. Kosten: ${}", 
                     costContext.getInputTokens(),
                     costContext.getOutputTokens(),
                     totalCost.toPlainString());
            
            // Modell erfassen
            result.setModel(costContext.getModel());
            
            // Kosten in die Antwort setzen
            result.setCost(totalCost);
            
            return result;
        } catch (Exception e) {
            log.error("Fehler bei der LLM-basierten Extraktion von Aufgaben aus PDF-Dateien", e);
            throw new RuntimeException("Fehler bei der Verarbeitung der PDF-Dateien: " + e.getMessage(), e);
        }
    }

}