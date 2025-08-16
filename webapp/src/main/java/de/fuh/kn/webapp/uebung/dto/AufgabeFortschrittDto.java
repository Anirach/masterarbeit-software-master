package de.fuh.kn.webapp.uebung.dto;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO zur Repräsentation des Fortschritts eines Studenten bei einer Aufgabe.
 * Diese Klasse wird verwendet, um den Fortschrittsstand einzelner Aufgaben für einen Studenten anzuzeigen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AufgabeFortschrittDto {
    
    /**
     * Die Aufgabe, für die der Fortschritt angezeigt wird.
     */
    private AufgabeDto aufgabe;
    
    /**
     * Die Anzahl der abgeschlossenen Teilaufgaben.
     */
    private Integer abgeschlosseneTeilaufgaben;
    
    /**
     * Der durchschnittliche Punktestand des Studenten bei dieser Aufgabe (0-100).
     */
    private Integer durchschnittlichePunktzahl;
    
    /**
     * Gibt an, ob die Aufgabe für den Studenten freigeschaltet ist.
     */
    private boolean freigeschaltet;
}