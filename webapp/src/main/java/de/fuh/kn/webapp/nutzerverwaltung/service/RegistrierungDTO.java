package de.fuh.kn.webapp.nutzerverwaltung.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object für die Registrierung neuer Nutzer.
 * Enthält alle Felder, die für die Registrierung eines Nutzers benötigt werden.
 */
@Getter
@Setter
public class RegistrierungDTO {

    /**
     * Die E-Mail-Adresse des Nutzers.
     * Wird für die Anmeldung verwendet.
     */
    @NotBlank(message = "E-Mail-Adresse ist erforderlich")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein")
    private String email;

    /**
     * Das Passwort des Nutzers.
     */
    @NotBlank(message = "Passwort ist erforderlich")
    @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein")
    private String passwort;
    
    /**
     * Bestätigung des Passworts.
     * Muss mit dem Passwort übereinstimmen.
     */
    @NotBlank(message = "Passwortbestätigung ist erforderlich")
    private String passwortBestaetigung;

    /**
     * Der Vorname des Nutzers.
     */
    @NotBlank(message = "Vorname ist erforderlich")
    private String vorname;

    /**
     * Der Nachname des Nutzers.
     */
    @NotBlank(message = "Nachname ist erforderlich")
    private String nachname;
    
    /**
     * Die Matrikelnummer des Studenten.
     * Wird nur für Studierende benötigt.
     */
    @NotBlank(message = "Matrikelnummer ist erforderlich")
    @Size(min = 7, max = 7, message = "Die Matrikelnummer muss 7 Zeichen lang sein")
    private String matrikelnummer;
}
