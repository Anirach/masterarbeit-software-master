package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Spezialisierung der Nutzer-Klasse für Kursbetreuende.
 * Kursbetreuende können Kurse verwalten, Aufgaben erstellen und Nutzer verwalten.
 */
@Entity
@DiscriminatorValue("KURSBETREUER")
@Getter
@Setter
public class Kursbetreuer extends Nutzer {
    // Keine zusätzlichen Attribute für Kursbetreuer
    // Die Rolle ist durch den Typ bestimmt
}
