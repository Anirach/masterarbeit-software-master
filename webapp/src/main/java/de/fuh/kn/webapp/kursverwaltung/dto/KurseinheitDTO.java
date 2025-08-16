package de.fuh.kn.webapp.kursverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO zur Repräsentation von Kurseinheit-Daten in der Benutzeroberfläche.
 * Diese Klasse enthält alle relevanten Informationen einer Kurseinheit für die Anzeige
 * und Bearbeitung in der Kurseinheitübersicht und Detailansicht.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KurseinheitDTO extends BaseDTO {
    
    /**
     * Der Name der Kurseinheit.
     */
    private String name;
    
    /**
     * Die Reihenfolge der Kurseinheit innerhalb des Kurses.
     */
    private Integer reihenfolge;
    
    /**
     * Die ID des zugehörigen Kurses.
     */
    private Long kursId;
    
    /**
     * Die Liste der Kursmaterialien, die dieser Kurseinheit zugeordnet sind.
     * Wird nur bei Bedarf befüllt, kann null sein.
     */
    private List<KursMaterialDTO> kursMaterialien = new ArrayList<>();
    
    /**
     * Die Anzahl der Aufgaben in dieser Kurseinheit.
     * Wird nur für die Übersichtsanzeige verwendet.
     */
    private Integer aufgabenCount;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity ("Kurseinheit")
     */
    @Override
    public String getEntityTypeName() {
        return "Kurseinheit";
    }

    @Override
    public String getEntityDisplayName() {
        return this.getName();
    }
}
