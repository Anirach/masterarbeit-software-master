package de.fuh.kn.webapp.nutzerverwaltung.service;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object für die Änderung des Passworts im Nutzerprofil.
 * Enthält das aktuelle Passwort zur Verifikation sowie das neue Passwort
 * und dessen Bestätigung.
 */
@Getter
@Setter
public class PasswortAenderungDTO {

    /**
     * Das aktuelle Passwort des Nutzers.
     * Wird zur Verifikation benötigt, bevor das Passwort geändert werden kann.
     */
    private String aktuellesPasswort;

    /**
     * Das neue Passwort des Nutzers.
     * Muss die gleichen Anforderungen erfüllen wie bei der Registrierung.
     */
    private String neuesPasswort;

    /**
     * Die Bestätigung des neuen Passworts.
     * Muss mit dem neuen Passwort übereinstimmen.
     */
    private String passwortBestaetigung;
}
