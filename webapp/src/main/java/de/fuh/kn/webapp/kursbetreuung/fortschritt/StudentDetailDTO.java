package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO für die Detailansicht des Fortschritts eines Studenten.
 * 
 * Enthält alle relevanten Informationen für die Kursbetreuung zur Bewertung
 * des Lernfortschritts eines einzelnen Studenten.
 */
@Data
@Builder
public class StudentDetailDTO {
    
    /**
     * Basis-Informationen über den Studenten
     */
    private Long studentId;
    private String studentName;
    private String studentEmail;
    
    /**
     * Kurs-Informationen
     */
    private Long kursId;
    private String kursName;
    
    /**
     * Fortschritts-Übersicht
     */
    private int gesamtTeilaufgaben;
    private int abgeschlosseneTeilaufgaben;
    private double fortschrittProzent;
    
    /**
     * Aktivitäts-Informationen
     */
    private long anzahlNachrichten;
    private BigDecimal aiKostenGesamt;
    private LocalDateTime letzteAktivitaet;
    
    /**
     * Detaillierte Lösungsversuche (nach Teilaufgaben gruppiert)
     */
    private List<TeilaufgabeDetailDTO> teilaufgaben;
    
    /**
     * Chat-Übersicht (nach Teilaufgaben gruppiert)
     */
    private List<ChatUebersichtDTO> chats;
}