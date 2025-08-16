package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO zur Repräsentation von Kurs-Daten in der Benutzeroberfläche.
 * Diese Klasse enthält alle relevanten Informationen eines Kurses für die Anzeige
 * und Bearbeitung in der Kursübersicht und Detailansicht.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KursDTO extends BaseDTO {
    
    /**
     * Der Name des Kurses.
     */
    private String name;
    
    /**
     * Die Liste der Kurseinheiten dieses Kurses.
     * Wird nur bei Bedarf befüllt, kann null sein.
     */
    private List<KurseinheitDTO> kurseinheiten = new ArrayList<>();
    
    /**
     * Die Liste der Kursmaterialien, die dem gesamten Kurs zugeordnet sind.
     * Wird nur bei Bedarf befüllt, kann null sein.
     */
    private List<KursMaterialDTO> kursMaterialien = new ArrayList<>();
    
    /**
     * Die Anzahl aller Belegungen dieses Kurses.
     */
    private Integer anzahlBelegungen;
    
    /**
     * Die Anzahl der aktiven Belegungen dieses Kurses.
     */
    private Integer anzahlAktiveBelegungen;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Kurs")
     */
    @Override
    public String getEntityTypeName() {
        return "Kurs";
    }

    @Override
    public String getEntityDisplayName() {
        return this.getName();
    }
}
