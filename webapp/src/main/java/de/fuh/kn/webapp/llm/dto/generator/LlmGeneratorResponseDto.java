package de.fuh.kn.webapp.llm.dto.generator;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO für Antworten der LLM-basierten Aufgabengenerierung.
 * Enthält die generierte Aufgabe sowie Metadaten zu Token-Nutzung und Kosten.
 */
@Getter
@Setter
public class LlmGeneratorResponseDto {
    
    /**
     * Die generierte Aufgabe.
     */
    private AufgabeDto aufgabe;
    
    /**
     * Anzahl der verbrauchten Input-Tokens.
     */
    private Integer inputTokens;
    
    /**
     * Anzahl der generierten Output-Tokens.
     */
    private Integer outputTokens;
    
    /**
     * Das verwendete Modell.
     */
    private String model;
    
    /**
     * Die Kosten für diese Anfrage.
     */
    private BigDecimal cost;
    
    /**
     * Fehlermeldung, falls die Generierung fehlgeschlagen ist.
     */
    private String errorMessage;
    
    /**
     * Prüft, ob die Generierung erfolgreich war.
     *
     * @return true, wenn keine Fehlermeldung vorhanden ist
     */
    public boolean isSuccessful() {
        return errorMessage == null || errorMessage.isEmpty();
    }
}