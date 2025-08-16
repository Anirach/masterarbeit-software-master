package de.fuh.kn.webapp.llm.dto.bewertung;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO für die Antwort auf eine Bewertungsanfrage.
 * Enthält die Bewertungsergebnisse und zusätzliche Metadaten zur LLM-Verwendung.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BewertungsResponseDto {

    /**
     * Die erreichte Punktzahl (0-100).
     */
    private Integer punkte;
    
    /**
     * Das Feedback zur Lösung.
     */
    private String feedback;
    
    /**
     * Die farbliche Bewertung der Felder.
     * Schlüssel ist der Name des Feldes, Wert ist die Farbkategorie (z.B. "red", "yellow", "green").
     */
    @Builder.Default
    private Map<String, String> felderBewertung = new HashMap<>();
    
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
     * Die ID des Lösungsversuchs, auf den sich diese Bewertung bezieht.
     * 
     * Wird bei Anfragen an das LLM ignoriert, aber kann vom aufrufenden Code gesetzt werden.
     */
    @JsonIgnore
    private Long loesungsversuchId;
}