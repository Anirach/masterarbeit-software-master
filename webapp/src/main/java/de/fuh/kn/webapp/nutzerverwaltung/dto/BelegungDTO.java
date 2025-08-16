package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Data Transfer Object für die Belegung-Entität.
 * Enthält die Informationen zur Zuordnung eines Studenten zu einem Kurs.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BelegungDTO extends BaseDTO {
    
    /**
     * Die ID des Studenten, der den Kurs belegt.
     */
    private Long studentId;
    
    /**
     * Die Matrikelnummer des Studenten, für einfachere Lesbarkeit.
     */
    private String matrikelnummer;
    
    /**
     * Der Name des Studenten, für einfachere Lesbarkeit.
     */
    private String studentName;
    
    /**
     * Die ID des belegten Kurses.
     */
    private Long kursId;
    
    /**
     * Der Name des belegten Kurses, für einfachere Lesbarkeit.
     */
    private String kursName;
    
    /**
     * Das Startdatum der Belegung.
     */
    private LocalDate startDatum;
    
    /**
     * Das Enddatum der Belegung.
     * Wenn null, ist die Belegung zeitlich unbegrenzt.
     */
    private LocalDate endDatum;
    
    /**
     * Gibt an, ob die Belegung aktuell gültig ist (aktuelles Datum liegt zwischen Start- und Enddatum).
     */
    private boolean aktiv;
    
    /**
     * Gibt an, ob der Student vollständig in der Anwendung registriert ist.
     * Bei false handelt es sich um einen temporären Dummy-Eintrag, der nur über die Matrikelnummer erfasst wurde.
     */
    private Boolean istRegistriert;
    
    /**
     * Der Fortschritt des Studenten in diesem Kurs in Prozent (0-100).
     * Null wenn der Fortschritt nicht berechnet wurde.
     */
    private Double fortschrittProzent;
    
    /**
     * Anzahl der abgeschlossenen Teilaufgaben.
     * Null wenn der Fortschritt nicht berechnet wurde.
     */
    private Integer abgeschlosseneTeilaufgaben;
    
    /**
     * Gesamtanzahl der Teilaufgaben im Kurs.
     * Null wenn der Fortschritt nicht berechnet wurde.
     */
    private Integer gesamtTeilaufgaben;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Belegung")
     */
    @Override
    public String getEntityTypeName() {
        return "Belegung";
    }

    @Override
    public String getEntityDisplayName() {
        throw new UnsupportedOperationException();
    }
}
