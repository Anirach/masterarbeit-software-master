package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.llm.dto.bewertung.FeedbackResponseDto;
import de.fuh.kn.webapp.llm.observability.OperationType;
import de.fuh.kn.webapp.llm.observability.TokenUsageObserver;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import de.fuh.kn.webapp.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service für die Bewertung von Lösungen über LLM-Integration.
 * Definiert die grundlegenden Operationen für die Interaktion mit dem LLM zur Bewertung von Aufgaben.
 * Implementierung für die Kommunikation mit OpenAI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmBewertungService {

    @Qualifier("evaluationChatClient")
    private final ChatModel evaluationChatClient;
    
    @Qualifier("explanationChatClient")
    private final ChatModel explanationChatClient;
    
    private final TokenUsageObserver tokenUsageObserver;

    /**
     * Bewertet eine eingereichte Lösung anhand der Musterlösung.
     *
     * @param requestDto Die Anfrage mit Aufgabenstellung, Musterlösung und eingereichter Lösung
     * @return Die Bewertung mit Punkten, Feedback, Feldbewertungen und Kosteninfo
     */
    public BewertungsResponseDto evaluateSolution(BewertungsRequestDto requestDto) {
        try {
            // Kostentracking starten mit Operationstyp
            tokenUsageObserver.startCostContext(OperationType.EVALUATION);
            
            // Converter für die strukturierte Ausgabe konfigurieren
            BeanOutputConverter<BewertungsResponseDto> converter = 
                    new BeanOutputConverter<>(BewertungsResponseDto.class);
            
            // Prompt mit Template und Parametern erstellen
            PromptTemplate template = new PromptTemplate(PromptTemplates.EVALUATION_TEMPLATE);
            
            // Eingabe und Musterlösung als Strings formatieren
            String musterloesungString = formatMap(requestDto.getMusterloesungFelder());
            String loesungString = formatMap(requestDto.getLoesungFelder());

            Map<String, Object> variables = new HashMap<>();
            variables.put("aufgabe", requestDto.getAufgabenstellungAufgabe() + "\n" + 
                    requestDto.getAufgabenstellungTeilaufgabe());
            variables.put("musterloesung", musterloesungString);
            variables.put("antwort", loesungString);
            variables.put("format", converter.getFormat());
            
            if (requestDto.getBewertungshinweise() != null && !requestDto.getBewertungshinweise().isBlank()) {
                variables.put("bewertungshinweise", requestDto.getBewertungshinweise());
            } else {
                variables.put("bewertungshinweise", "");
            }
            
            Prompt prompt = template.create(variables);
            
            // LLM aufrufen
            ChatResponse response = evaluationChatClient.call(prompt);
            String responseText = response.getResult().getOutput().getText();
            
            // Backslashes in LaTeX-Ausdrücken für JSON escapen
            String sanitizedResponseText = JsonUtil.sanitizeJsonResponse(responseText);
            
            // Strukturierte Ausgabe konvertieren
            BewertungsResponseDto result = converter.convert(sanitizedResponseText);
            
            // Tokens erfassen
            Usage usage = response.getMetadata().getUsage();
            result.setInputToken(usage.getPromptTokens());
            result.setOutputToken(usage.getCompletionTokens());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            BigDecimal totalCost = costContext.getTotalCost();
            log.info("Bewertung durchgeführt für Student {} mit {} Input-Token und {} Output-Token. Kosten: ${}", 
                     requestDto.getStudentId() != null ? requestDto.getStudentId() : "unbekannt",
                     costContext.getInputTokens(),
                     costContext.getOutputTokens(),
                     totalCost.toPlainString());
            
            // Modell erfassen
            result.setModel(costContext.getModel());
            
            // Kosten in die Antwort setzen
            result.setCost(totalCost);
            
            return result;
        } catch (Exception e) {
            log.error("Fehler bei der Bewertung der Lösung", e);
            throw new RuntimeException("Fehler bei der Bewertung der Lösung", e);
        }
    }

    /**
     * Generiert detailliertes Feedback basierend auf der Punktzahl.
     *
     * @param requestDto Die Anfrage mit Aufgabenstellung, Musterlösung und eingereichter Lösung
     * @param score Die erreichte Punktzahl
     * @return Ein detailliertes Feedback mit Metadaten und Kosteninfo
     */
    public FeedbackResponseDto generateFeedback(BewertungsRequestDto requestDto, int score) {
        try {
            // Kostentracking starten mit Operationstyp
            tokenUsageObserver.startCostContext(OperationType.FEEDBACK);
            
            // Prompt mit Template und Parametern erstellen
            PromptTemplate template = new PromptTemplate(PromptTemplates.FEEDBACK_TEMPLATE);
            
            // Eingabe und Musterlösung als Strings formatieren
            String musterloesungString = formatMap(requestDto.getMusterloesungFelder());
            String loesungString = formatMap(requestDto.getLoesungFelder());
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("aufgabe", requestDto.getAufgabenstellungAufgabe() + "\n" + 
                    requestDto.getAufgabenstellungTeilaufgabe());
            variables.put("musterloesung", musterloesungString);
            variables.put("antwort", loesungString);
            variables.put("punkte", score);
            
            Prompt prompt = template.create(variables);
            
            // LLM aufrufen
            ChatResponse response = evaluationChatClient.call(prompt);
            String feedback = response.getResult().getOutput().getText();
            
            // DTO erstellen
            FeedbackResponseDto result = new FeedbackResponseDto();
            result.setFeedback(feedback);
            result.setScore(score);
            
            // Tokens erfassen
            Usage usage = response.getMetadata().getUsage();
            result.setInputToken(usage.getPromptTokens());
            result.setOutputToken(usage.getCompletionTokens());
            
            // Kostentracking beenden
            TokenUsageObserver.CostContext costContext = tokenUsageObserver.endCostContext();
            BigDecimal totalCost = costContext.getTotalCost();
            log.info("Feedback generiert für Student {} mit {} Input-Token und {} Output-Token. Kosten: ${}", 
                     requestDto.getStudentId() != null ? requestDto.getStudentId() : "unbekannt",
                     costContext.getInputTokens(),
                     costContext.getOutputTokens(),
                     totalCost.toPlainString());
            
            // Modell erfassen
            result.setModel(costContext.getModel());
            
            // Kosten in die Antwort setzen
            result.setCost(totalCost);
            
            return result;
        } catch (Exception e) {
            log.error("Fehler bei der Generierung des Feedbacks", e);
            throw new RuntimeException("Fehler bei der Generierung des Feedbacks", e);
        }
    }


    
    /**
     * Formatiert eine Map zu einem String für den Prompt.
     * 
     * @param map Die zu formatierende Map
     * @return Ein formatierter String
     */
    private String formatMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey())
              .append(": ")
              .append(entry.getValue())
              .append("\n");
        }
        return sb.toString();
    }
    

}