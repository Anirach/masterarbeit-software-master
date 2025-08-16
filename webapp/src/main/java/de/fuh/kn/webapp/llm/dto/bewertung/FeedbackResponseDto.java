package de.fuh.kn.webapp.llm.dto.bewertung;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO für die Antwort auf eine Feedback-Anfrage.
 * Enthält das generierte Feedback und Metadaten zur LLM-Nutzung.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDto {

    /**
     * Das vom LLM generierte Feedback.
     */
    private String feedback;
    
    /**
     * Die Punktzahl, auf der das Feedback basiert (0-100).
     */
    private Integer score;
    
    /**
     * Die Anzahl der verwendeten Input-Token.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber in der Antwort vom Service gefüllt.
     */
    @JsonIgnore
    private Integer inputToken;
    
    /**
     * Die Anzahl der verwendeten Output-Token.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber in der Antwort vom Service gefüllt.
     */
    @JsonIgnore
    private Integer outputToken;
    
    /**
     * Das verwendete LLM-Modell.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber in der Antwort vom Service gefüllt.
     */
    @JsonIgnore
    private String model;
    
    /**
     * Die Kosten für die LLM-Anfrage in USD.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber in der Antwort vom Service gefüllt.
     */
    @JsonIgnore
    private BigDecimal cost;
    
    /**
     * Die ID des Lösungsversuchs, auf den sich dieses Feedback bezieht.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber kann vom aufrufenden Code gesetzt werden.
     */
    @JsonIgnore
    private Long loesungsversuchId;
}