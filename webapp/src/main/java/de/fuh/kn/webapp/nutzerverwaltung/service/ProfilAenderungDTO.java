package de.fuh.kn.webapp.nutzerverwaltung.service;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object für die Änderung von Benutzerdaten im Profil.
 * Enthält die Felder, die ein Benutzer in seinem Profil ändern kann:
 * E-Mail, Vorname und Nachname.
 */
@Getter
@Setter
public class ProfilAenderungDTO {

    /**
     * Die E-Mail-Adresse des Nutzers.
     * Kann vom Benutzer geändert werden.
     */
    private String email;

    /**
     * Der Vorname des Nutzers.
     * Kann vom Benutzer geändert werden.
     */
    private String vorname;

    /**
     * Der Nachname des Nutzers.
     * Kann vom Benutzer geändert werden.
     */
    private String nachname;
}
