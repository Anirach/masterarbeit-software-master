package de.fuh.kn.webapp.nutzerverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.*;

/**
 * Data Transfer Object für die Nutzer-Entität.
 * Enthält die grundlegenden Attribute eines Nutzers, unabhängig vom Typ.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class NutzerDTO extends BaseDTO {

    /**
     * Die E-Mail-Adresse des Nutzers.
     * Wird für die Anmeldung verwendet.
     */
    private String email;

    /**
     * Der Vorname des Nutzers.
     */
    private String vorname;

    /**
     * Der Nachname des Nutzers.
     */
    private String nachname;
    
    /**
     * Flag, das angibt, ob der Nutzer vollständig registriert ist.
     */
    private Boolean istRegistriert;
    
    /**
     * Anzeigename des Nutzers für die Darstellung in der Benutzeroberfläche.
     * Wird automatisch generiert basierend auf den Nutzer-Attributen.
     */
    private String displayName;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Nutzer")
     */
    @Override
    public String getEntityTypeName() {
        return "Nutzer";
    }

    @Override
    public String getEntityDisplayName() {
        return this.getDisplayName();
    }
}
