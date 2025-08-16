package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO für die Fortschrittsübersicht eines Studenten in einem Kurs.
 * 
 * Dieses DTO aggregiert verschiedene Metriken zum Lernfortschritt:
 * - Persönliche Daten des Studenten
 * - Belegungsstatus
 * - Aufgabenfortschritt
 * - Chat-Aktivität
 * - KI-Nutzungskosten
 * - Letzte Aktivität
 */
@Data
@Builder
public class FortschrittOverviewDTO {
    /**
     * ID der Belegung
     */
    private Long belegungId;
    
    /**
     * ID des Studenten
     */
    private Long studentId;
    
    /**
     * Vollständiger Name des Studenten
     */
    private String studentName;
    
    /**
     * E-Mail-Adresse des Studenten
     */
    private String studentEmail;
    
    /**
     * Ob die Belegung aktiv ist
     */
    private boolean aktiv;
    
    /**
     * Datum der Belegung
     */
    private LocalDateTime belegungDatum;
    
    // Fortschrittsdaten
    /**
     * Gesamtanzahl der Teilaufgaben im Kurs
     */
    private int gesamtAufgaben;
    
    /**
     * Anzahl der abgeschlossenen Teilaufgaben
     */
    private int abgeschlosseneAufgaben;
    
    /**
     * Fortschritt in Prozent
     */
    private double fortschrittProzent;
    
    // Chat-Aktivität
    /**
     * Anzahl der vom Studenten gesendeten Chat-Nachrichten
     */
    private long anzahlNachrichten;
    
    // KI-Nutzungskosten
    /**
     * Gesamte KI-Kosten für diesen Studenten in diesem Kurs
     * (Aktuell noch Platzhalter-Implementierung)
     */
    private BigDecimal aiKostenGesamt;
    
    // Zusätzliche Metriken
    /**
     * Zeitpunkt der letzten Aktivität (letzter Lösungsversuch)
     */
    private LocalDateTime letzteAktivitaet;
}