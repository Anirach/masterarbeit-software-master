package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO für den Export von Teilaufgaben.
 * Enthält alle relevanten Informationen für eine Teilaufgabe, die exportiert werden sollen.
 */
@Getter
@Setter
public class TeilaufgabeExportDTO {

    /**
     * Die ID der Teilaufgabe.
     */
    private Long id;

    /**
     * Die Reihenfolge der Teilaufgabe innerhalb der Aufgabe.
     */
    private Integer reihenfolge;
    
    /**
     * Die Aufgabenstellung im Markdown-Format.
     * Enthält auch die Platzhalter für die Eingabefelder.
     */
    private String aufgabenstellungMarkdown;
    
    /**
     * Die Musterlösung für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die erwartete Lösung.
     */
    private Map<String, String> musterloesungFelder = new HashMap<>();
    
    /**
     * Die Bewertungshinweise für die Aufgabe.
     */
    private String musterloesungBewertungshinweise;
}