package de.fuh.kn.webapp.aufgabenverwaltung.dto.export;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO für den Export von Aufgaben.
 * Enthält alle relevanten Informationen für eine Aufgabe, die exportiert werden sollen.
 */
@Getter
@Setter
public class AufgabeExportDTO {

    /**
     * Die ID der Aufgabe.
     */
    private Long id;

    /**
     * Der Titel der Aufgabe.
     */
    private String titel;
    
    /**
     * Gibt an, ob die Aufgabe eine einfache Aufgabe ist (mit nur einer Teilaufgabe).
     */
    private boolean einfach;
    
    /**
     * Der allgemeine Aufgabentext für Aufgaben mit mehreren Teilaufgaben.
     * Bei einfachen Aufgaben ist dieser null.
     */
    private String aufgabenText;
    
    /**
     * Die Reihenfolge der Aufgabe innerhalb der Kurseinheit.
     */
    private Integer reihenfolge;

    /**
     * Die ID der zugehörigen Kurseinheit.
     */
    private Long kurseinheitId;

    /**
     * Die Teilaufgaben der Aufgabe.
     */
    private List<TeilaufgabeExportDTO> teilaufgaben = new ArrayList<>();
}