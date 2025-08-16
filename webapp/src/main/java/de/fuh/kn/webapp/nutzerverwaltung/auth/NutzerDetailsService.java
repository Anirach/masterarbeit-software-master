package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service zur Authentifizierung von Nutzern über das Spring Security Framework.
 * Lädt Nutzer-Details aus der Datenbank und konvertiert sie in spezifische UserDetails-Objekte.
 */
@Service
public class NutzerDetailsService implements UserDetailsService {

    private final NutzerService nutzerService;

    /**
     * Konstruktor mit Dependency Injection des NutzerService.
     *
     * @param nutzerService Der Service für den Zugriff auf Nutzer-Daten.
     */
    @Autowired
    public NutzerDetailsService(NutzerService nutzerService) {
        this.nutzerService = nutzerService;
    }

    /**
     * Lädt einen Nutzer anhand seiner E-Mail-Adresse und erstellt ein spezifisches UserDetails-Objekt.
     * Je nach Nutzertyp wird ein StudentUserDetails oder KursbetreuerUserDetails zurückgegeben.
     * Prüft, ob der Nutzer vollständig registriert ist (istRegistriert = true).
     *
     * @param email Die E-Mail-Adresse des Nutzers als Benutzername.
     * @return Ein UserDetails-Objekt mit den Nutzer-Informationen.
     * @throws UsernameNotFoundException wenn kein Nutzer mit der angegebenen E-Mail-Adresse gefunden wurde
     *                                   oder wenn der Nutzer nicht registriert ist.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        NutzerDTO nutzerDTO = nutzerService.findeNutzerNachEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Nutzer mit E-Mail " + email + " nicht gefunden"));
        
        // Prüfen, ob der Nutzer registriert ist
        if (!nutzerDTO.getIstRegistriert()) {
            throw new UsernameNotFoundException("Nutzer mit E-Mail " + email + " ist nicht vollständig registriert");
        }
        
        // Passwort aus der Datenbank laden
        String passwort = nutzerService.findePasswortNachEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Passwort für Nutzer mit E-Mail " + email + " nicht gefunden"));
        
        // Je nach Nutzertyp entsprechendes DTO und UserDetails erstellen
        if (nutzerDTO instanceof StudentDTO studentDTO) {
            return new StudentUserDetails(studentDTO, passwort);
        } else if (nutzerDTO instanceof KursbetreuerDTO kursbetreuerDTO) {
            return new KursbetreuerUserDetails(kursbetreuerDTO, passwort);
        } else {
            throw new IllegalArgumentException("Unbekannter Nutzertyp: " + nutzerDTO.getEntityTypeName());
        }
    }
}
