package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO für die Übersicht der Chat-Aktivitäten einer Teilaufgabe.
 * 
 * Gruppiert alle Chat-Nachrichten zu einer Teilaufgabe für die Fortschrittsübersicht.
 */
@Data
@Builder
public class ChatUebersichtDTO {
    
    /**
     * Teilaufgaben-Informationen
     */
    private Long teilaufgabeId;
    private String aufgabeTitel;
    private String kurseinheitName;
    private int teilaufgabeReihenfolge;
    
    /**
     * Chat-Informationen
     */
    private Long chatId;
    private int anzahlNachrichten;
    private LocalDateTime letzteNachrichtZeitpunkt;
    private BigDecimal gesamtKosten;
    
    /**
     * Nachrichten im Chat (für Detailansicht)
     */
    private List<ChatNachrichtDetailDTO> nachrichten;
}