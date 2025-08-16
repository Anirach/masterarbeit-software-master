package de.fuh.kn.webapp.aufgabenverwaltung.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Listener für erfolgreiche Authentifizierungen, der speziell übersprungene Aufgaben zurücksetzt.
 * Bei jedem Login eines Studenten werden alle Lösungsversuche, die als übersprungen aber nicht als
 * abgeschlossen markiert sind, zurückgesetzt. Dadurch muss der Student bei jeder neuen Session
 * neu entscheiden, ob er eine Aufgabe überspringen oder lösen möchte.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkippedAssignmentResetListener {

    private final LoesungsversuchService loesungsversuchService;

    /**
     * Reagiert auf erfolgreiche Authentifizierungsereignisse und setzt übersprungene
     * Lösungsversuche zurück.
     *
     * @param event Das Authentifizierungsereignis
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Nutzer aus dem Authentication-Objekt extrahieren
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();

            // Prüfen, ob es sich um einen Studenten handelt
            if (isStudent(userDetails)) {
                try {
                    // Studenten anhand der Email finden und Lösungsversuche zurücksetzen
                    loesungsversuchService.findeStudentIdByEmail(username)
                        .ifPresent(studentId -> loesungsversuchService.setzeUebersprungeneLoesungsversucheZurueck(studentId));
                } catch (Exception e) {
                    log.error("Fehler beim Zurücksetzen der übersprungenen Lösungsversuche für Student {}: {}",
                            username, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Überprüft, ob es sich bei dem angemeldeten Nutzer um einen Studenten handelt.
     * Im Produktionscode wird geprüft, ob es sich um ein StudentUserDetails-Objekt handelt.
     * In Tests kann dieser Methode überschrieben werden, um studentenspezifisches Verhalten zu testen.
     *
     * @param userDetails Die UserDetails des authentifizierten Nutzers
     * @return true wenn der Nutzer ein Student ist, false sonst
     */
    protected boolean isStudent(UserDetails userDetails) {
        return userDetails instanceof de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
    }
}