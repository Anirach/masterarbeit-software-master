package de.fuh.kn.webapp.kursverwaltung.dto.export;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO für den Export von Kurs-Daten.
 * Enthält alle für den Export relevanten Informationen eines Kurses,
 * einschließlich aller Kurseinheiten, Kursmaterialien und Aufgaben.
 */
@Getter
@Setter
public class KursExportDTO {
    
    /**
     * Die ID des Kurses.
     */
    private Long id;
    
    /**
     * Der Name des Kurses.
     */
    private String name;
    
    /**
     * Die Liste der Kurseinheiten dieses Kurses.
     */
    private List<KurseinheitExportDTO> kurseinheiten = new ArrayList<>();
    
    /**
     * Die Liste der Kursmaterialien, die dem gesamten Kurs zugeordnet sind.
     */
    private List<KursMaterialExportDTO> kursMaterialien = new ArrayList<>();
    
    /**
     * Die Liste aller Aufgaben des Kurses (aus allen Kurseinheiten).
     */
    private List<AufgabeExportDTO> aufgaben = new ArrayList<>();
}