package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO für die Detailansicht einer Teilaufgabe im Kontext der Fortschrittsübersicht.
 * 
 * Enthält Informationen über eine Teilaufgabe und alle zugehörigen Lösungsversuche
 * des Studenten.
 */
@Data
@Builder
public class TeilaufgabeDetailDTO {
    
    /**
     * Teilaufgaben-Informationen
     */
    private Long teilaufgabeId;
    private String aufgabeTitel;
    private String kurseinheitName;
    private int teilaufgabeReihenfolge;
    
    /**
     * Status der Teilaufgabe für den Studenten
     */
    private boolean istAbgeschlossen;
    private boolean istUebersprungen;
    
    /**
     * Alle Lösungsversuche zu dieser Teilaufgabe
     */
    private List<LoesungsVersuchDetailDTO> loesungsversuche;
    
    /**
     * Anzahl der Lösungsversuche
     */
    private int anzahlVersuche;
}