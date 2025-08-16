package de.fuh.kn.webapp.aufgabenverwaltung.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO für die Aufgabe-Entität.
 * Enthält alle relevanten Informationen für eine Aufgabe.
 */
@Getter
@Setter
public class AufgabeDto extends BaseDTO {

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
    private List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();

    @Override
    public String getEntityTypeName() {
        return "Aufgabe";
    }

    @Override
    public String getEntityDisplayName() {
        return this.getTitel();
    }
}
