package de.fuh.kn.webapp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstrakte Basisklasse für alle DTOs im System, die zu Entitäten gehören.
 * Hilft bei der Identifizierung von DTOs in der Aktivitätsprotokollierung.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseDTO {

    /**
     * Die eindeutige ID der Entität, zu der dieses DTO gehört.
     */
    private Long id;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     * Diese Methode muss von jeder DTO-Klasse implementiert werden, 
     * um die korrekte Zuordnung zur Entity-Klasse zu ermöglichen.
     *
     * @return Der Klassenname der zugehörigen Entity
     */
    public abstract String getEntityTypeName();

    /**
     * Gibt den Anzeigenamen des zugehörigen Entity zurück, z.B. den Namen eines Kurses oder den Dateinamen eines Materials.
     * @return Anzeigename eines Entities
     */
    public abstract String getEntityDisplayName();
}
