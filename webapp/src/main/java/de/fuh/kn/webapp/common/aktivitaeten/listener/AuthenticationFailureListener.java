package de.fuh.kn.webapp.common.aktivitaeten.listener;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Listener für fehlgeschlagene Authentifizierungen.
 * Protokolliert fehlgeschlagene Anmeldeversuche in der Aktivitätsprotokollierung.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFailureListener {

    private final NutzerService nutzerService;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Reagiert auf fehlgeschlagene Authentifizierungsereignisse.
     *
     * @param event Das Authentifizierungsereignis
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        // Nutzername aus dem Authentication-Objekt extrahieren
        String username = event.getAuthentication().getName();
        
        try {
            // Aktivität ohne Nutzer-Objekt protokollieren (da die Authentifizierung fehlgeschlagen ist)
            String beschreibung = "Fehlgeschlagene Anmeldung für Benutzer: " + username;
            
            // Details zur fehlgeschlagenen Anmeldung sammeln
            Map<String, Object> details = new HashMap<>();
            details.put("username", username);
            details.put("fehlerklasse", event.getException().getClass().getSimpleName());
            details.put("fehlermeldung", event.getException().getMessage());
            
            // IP-Adresse und Session-ID extrahieren, falls verfügbar
            if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails authDetails) {
                details.put("remoteAddress", authDetails.getRemoteAddress());
                details.put("sessionId", authDetails.getSessionId());
            }

            //Nutzer nur verwenden, wenn es für diesen Nutzernamen einen Nutzer gibt
            NutzerDTO nutzerDTO = null;
            Optional<NutzerDTO> optionalNutzer = nutzerService.findeNutzerNachEmail(username);
            if(optionalNutzer.isPresent()){
                nutzerDTO = optionalNutzer.get();
            }
            
            aktivitaetsProtokollierungService.protokolliereAktivitaet(
                    nutzerDTO,
                    AktivitaetsTyp.LOGIN, // Trotzdem LOGIN als Typ verwenden
                    beschreibung,
                    details,
                    false, // Nicht erfolgreich
                    null,
                    null
            );
            
            log.warn("Fehlgeschlagene Anmeldung protokolliert für Benutzer: {}", username);
        } catch (Exception e) {
            log.error("Fehler beim Protokollieren der fehlgeschlagenen Anmeldung für {}: {}", 
                    username, e.getMessage(), e);
        }
    }
}
