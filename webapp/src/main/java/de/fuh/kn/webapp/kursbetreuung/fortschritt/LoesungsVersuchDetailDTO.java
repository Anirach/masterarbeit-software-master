package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO für die Detailansicht eines Lösungsversuchs.
 * 
 * Enthält alle relevanten Informationen für die Kursbetreuung zur Bewertung
 * eines einzelnen Lösungsversuchs eines Studenten.
 */
@Data
@Builder
public class LoesungsVersuchDetailDTO {
    
    /**
     * Basis-Informationen
     */
    private Long id;
    private LocalDateTime zeitpunkt;
    
    /**
     * Status des Lösungsversuchs
     */
    private boolean istAbgeschlossen;
    private boolean istUebersprungen;
    private boolean istZurueckGesetzt;
    
    /**
     * Bewertung
     */
    private Integer bewertungPunkte;
    private String bewertungFeedback;
    private Map<String, String> bewertungFelderFarbe;
    
    /**
     * Eingaben des Studenten
     */
    private Map<String, String> loesungFelder;
    
    /**
     * KI-Kosten für diesen Versuch
     */
    private BigDecimal kosten;
}