package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO für die Lösungsversuche der Studenten zu Teilaufgaben.
 * Wird verwendet, um Lösungsversuche zwischen Service-Schicht und Controller zu übertragen.
 */
@Getter
@Setter
public class LoesungsVersuchDTO {
    
    /**
     * Die ID des Lösungsversuchs.
     */
    private Long id;
    
    /**
     * Der Zeitpunkt des Lösungsversuchs.
     */
    private LocalDateTime zeitpunkt;
    
    /**
     * Die eingegebenen Lösungen für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die eingegebene Lösung.
     */
    private Map<String, String> loesungFelder = new HashMap<>();
    
    /**
     * Die erreichte Punktzahl (0-100).
     */
    private Integer bewertungPunkte;
    
    /**
     * Das Feedback zur Lösung.
     */
    private String bewertungFeedback;
    
    /**
     * Die farbliche Bewertung der Felder (rot, gelb, grün).
     * Schlüssel ist der Name des Feldes, Wert ist die Farbkategorie.
     */
    private Map<String, String> bewertungFelderFarbe = new HashMap<>();
    
    /**
     * Gibt an, ob der Lösungsversuch abgeschlossen wurde.
     * Wenn true, ist die Teilaufgabe erledigt.
     */
    private Boolean istAbgeschlossen = false;
    
    /**
     * Gibt an, ob der Lösungsversuch übersprungen wurde.
     * Wenn true, kann der Student mit der nächsten Aufgabe fortfahren.
     */
    private Boolean istUebersprungen = false;
    
    /**
     * Gibt an, ob der Lösungsversuch zurückgesetzt wurde.
     * Wenn true, wird der Lösungsversuch nicht mehr für die Vorbefüllung verwendet.
     */
    private Boolean istZurueckGesetzt = false;
    
    /**
     * Die Anzahl der verwendeten LLM-Token für die Bewertung.
     */
    private Integer llmToken;
    
    /**
     * Die ID des zugehörigen Studenten.
     */
    private Long studentId;
    
    /**
     * Die ID der zugehörigen Teilaufgabe.
     */
    private Long teilaufgabeId;
}