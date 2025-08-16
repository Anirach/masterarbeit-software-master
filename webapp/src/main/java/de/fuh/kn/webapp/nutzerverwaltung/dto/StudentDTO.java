package de.fuh.kn.webapp.nutzerverwaltung.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object für die Student-Entität.
 * Erweitert NutzerDTO um die zusätzlichen Attribute eines Studenten.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class StudentDTO extends NutzerDTO {

    /**
     * Die Matrikelnummer des Studenten.
     * Wird für die Zuordnung zu Kursen verwendet.
     */
    private String matrikelnummer;
    
    /**
     * IDs der Belegungen des Studenten.
     * Kann für einfache Übersichten verwendet werden, ohne die vollständigen Belegungsdaten zu laden.
     */
    private List<Long> belegungIds;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Student")
     */
    @Override
    public String getEntityTypeName() {
        return "Student";
    }
}
