package de.fuh.kn.webapp.nutzerverwaltung.dto;

import lombok.*;

/**
 * Data Transfer Object für die Kursbetreuer-Entität.
 * Erweitert NutzerDTO um die zusätzlichen Attribute eines Kursbetreuers.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KursbetreuerDTO extends NutzerDTO {
    /**
     * Temporäres Klartextpasswort, nur für die Anzeige nach der Erstellung eines neuen Kursbetreuers.
     * Wird nicht in der Datenbank gespeichert.
     */
    private String klartext_passwort;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Kursbetreuer")
     */
    @Override
    public String getEntityTypeName() {
        return "Kursbetreuer";
    }
}
