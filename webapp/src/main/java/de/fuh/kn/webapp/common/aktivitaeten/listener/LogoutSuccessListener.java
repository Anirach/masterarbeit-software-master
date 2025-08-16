package de.fuh.kn.webapp.common.aktivitaeten.listener;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Listener für erfolgreiche Abmeldungen.
 * Protokolliert Logouts in der Aktivitätsprotokollierung.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutSuccessListener {

    private final NutzerService nutzerService;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Reagiert auf erfolgreiche Logout-Ereignisse.
     *
     * @param event Das Logout-Ereignis
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        // Authentication aus dem Event holen
        Authentication authentication = event.getAuthentication();
        
        if (authentication != null) {
            String username = authentication.getName();
            
            try {
                // Nutzer direkt über die E-Mail (Username) laden
                Optional<NutzerDTO> nutzerOpt = nutzerService.findeNutzerNachEmail(username);
                NutzerDTO nutzer = null;
                if (nutzerOpt.isPresent()) {
                    // Logout mit Nutzer protokollieren
                    nutzer = nutzerOpt.get();
                }

                String beschreibung = "Abmeldung für Benutzer: " + username;
                Map<String, Object> details = new HashMap<>();
                details.put("username", username);

                if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails authDetails) {
                    details.put("remoteAddress", authDetails.getRemoteAddress());
                    details.put("sessionId", authDetails.getSessionId());
                }

                aktivitaetsProtokollierungService.protokolliereAktivitaet(
                        nutzer, // Kein Nutzer
                        AktivitaetsTyp.LOGOUT,
                        beschreibung,
                        details,
                        true,
                        null,
                        null
                );

            } catch (Exception e) {
                log.error("Fehler beim Protokollieren des Logouts für {}: {}", 
                        username, e.getMessage(), e);
            }
        } else {
            log.warn("Logout-Event ohne Authentication erhalten");
        }
    }
}
