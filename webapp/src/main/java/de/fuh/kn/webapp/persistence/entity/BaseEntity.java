package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstrakte Basisklasse für alle Entitäten im System.
 * Definiert gemeinsame Felder und Verhalten für alle Entitäten.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * Die eindeutige ID der Entität, wird automatisch generiert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
