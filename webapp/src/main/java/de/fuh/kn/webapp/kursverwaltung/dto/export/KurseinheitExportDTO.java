package de.fuh.kn.webapp.kursverwaltung.dto.export;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO für den Export von Kurseinheit-Daten.
 * Enthält alle für den Export relevanten Informationen einer Kurseinheit.
 */
@Getter
@Setter
public class KurseinheitExportDTO {
    
    /**
     * Die ID der Kurseinheit.
     */
    private Long id;
    
    /**
     * Der Name der Kurseinheit.
     */
    private String name;
    
    /**
     * Die Reihenfolge der Kurseinheit innerhalb des Kurses.
     */
    private Integer reihenfolge;
    
    /**
     * Die Liste der Kursmaterialien, die dieser Kurseinheit zugeordnet sind.
     */
    private List<KursMaterialExportDTO> kursMaterialien = new ArrayList<>();
}