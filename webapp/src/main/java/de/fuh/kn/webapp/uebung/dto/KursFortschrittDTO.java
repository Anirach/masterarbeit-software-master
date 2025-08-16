package de.fuh.kn.webapp.uebung.dto;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO zur Repräsentation des Fortschritts eines Studenten in einem Kurs.
 * Diese Klasse wird verwendet, um den Fortschritt eines Studenten in einem Kurs anzuzeigen,
 * ohne dass das KursDTO selbst angepasst werden muss.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KursFortschrittDTO {
    
    /**
     * Der Kurs, für den der Fortschritt angezeigt wird.
     */
    private KursDTO kurs;
    
    /**
     * Der Fortschritt des Studenten in diesem Kurs (in Prozent).
     */
    private Integer fortschrittProzent;
    
    /**
     * Anzahl der abgeschlossenen Teilaufgaben in diesem Kurs.
     */
    private Long abgeschlosseneTeilaufgaben;
    
    /**
     * Gesamtzahl der Teilaufgaben in diesem Kurs.
     */
    private Long gesamtTeilaufgaben;
    
    /**
     * Gibt an, ob der Kurs aktuell belegt ist (aktive Belegung).
     */
    private boolean istAktiv;
}