package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Die LoesungsVersuch-Entität repräsentiert einen Versuch eines Studenten,
 * eine Teilaufgabe zu lösen. Enthält die eingegebenen Lösungen und die Bewertung.
 */
@Entity
@Getter
@Setter
public class LoesungsVersuch extends BaseEntity {
    
    /**
     * Der Zeitpunkt des Lösungsversuchs.
     */
    @Column(nullable = false)
    private LocalDateTime zeitpunkt;
    
    /**
     * Die eingegebenen Lösungen für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die eingegebene Lösung.
     */
    @ElementCollection
    @CollectionTable(name = "loesungsversuch_felder", 
        joinColumns = @JoinColumn(name = "loesungsversuch_id"))
    @MapKeyColumn(name = "feld_name")
    @Column(name = "loesung", columnDefinition = "TEXT")
    private Map<String, String> loesungFelder = new HashMap<>();
    
    /**
     * Die erreichte Punktzahl (0-100).
     */
    @Column
    private Integer bewertungPunkte;
    
    /**
     * Das Feedback zur Lösung.
     */
    @Column(columnDefinition = "TEXT")
    private String bewertungFeedback;
    
    /**
     * Die farbliche Bewertung der Felder (rot, gelb, grün).
     * Schlüssel ist der Name des Feldes, Wert ist die Farbkategorie.
     */
    @ElementCollection
    @CollectionTable(name = "loesungsversuch_bewertungen", 
        joinColumns = @JoinColumn(name = "loesungsversuch_id"))
    @MapKeyColumn(name = "feld_name")
    @Column(name = "farbe")
    private Map<String, String> bewertungFelderFarbe = new HashMap<>();
    
    /**
     * Gibt an, ob der Lösungsversuch abgeschlossen wurde.
     * Wenn true, ist die Teilaufgabe erledigt.
     */
    @Column
    private Boolean istAbgeschlossen = false;
    
    /**
     * Gibt an, ob der Lösungsversuch übersprungen wurde.
     * Wenn true, kann der Student mit der nächsten Aufgabe fortfahren.
     */
    @Column
    private Boolean istUebersprungen = false;
    
    /**
     * Gibt an, ob der Lösungsversuch zurückgesetzt wurde.
     * Wenn true, wird der Lösungsversuch nicht mehr für die Vorbefüllung verwendet.
     */
    @Column
    private Boolean istZurueckGesetzt = false;
    
    /**
     * Die Anzahl der verwendeten Input-Token für die Bewertung.
     */
    @Column
    private Integer inputToken;
    
    /**
     * Die Anzahl der generierten Output-Token für die Bewertung.
     */
    @Column
    private Integer outputToken;
    
    /**
     * Das für die Bewertung verwendete Modell.
     */
    @Column
    private String modell;
    
    /**
     * Die berechneten Kosten für die Bewertung in USD.
     */
    @Column(precision = 10, scale = 6)
    private BigDecimal kosten;
    
    /**
     * Der zugehörige Student.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    /**
     * Die zugehörige Teilaufgabe.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teilaufgabe_id", nullable = false)
    private Teilaufgabe teilaufgabe;
}