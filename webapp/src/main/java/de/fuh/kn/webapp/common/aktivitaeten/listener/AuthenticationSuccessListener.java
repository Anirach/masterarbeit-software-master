package de.fuh.kn.webapp.common.aktivitaeten.listener;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener für erfolgreiche Authentifizierungen.
 * Protokolliert erfolgreiche Anmeldungen in der Aktivitätsprotokollierung.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationSuccessListener {

    private final NutzerService nutzerService;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Reagiert auf erfolgreiche Authentifizierungsereignisse.
     *
     * @param event Das Authentifizierungsereignis
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Nutzer aus dem Authentication-Objekt extrahieren
        Object principal = event.getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            
            try {
                // Nutzer aus dem NutzerRepository laden
                nutzerService.findeNutzerNachEmail(username).ifPresent(nutzer -> {
                    // Details zur erfolgreichen Anmeldung sammeln
                    Map<String, Object> details = new HashMap<>();
                    
                    // IP-Adresse und Session-ID extrahieren, falls verfügbar
                    if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails authDetails) {
                        details.put("remoteAddress", authDetails.getRemoteAddress());
                        details.put("sessionId", authDetails.getSessionId());
                    }
                    
                    // Nutzertyp hinzufügen
                    details.put("nutzerTyp", nutzer.getClass().getSimpleName());
                    
                    // Rollen hinzufügen
                    details.put("authorities", event.getAuthentication().getAuthorities());
                    
                    // Login mit Details protokollieren
                    aktivitaetsProtokollierungService.protokolliereAktivitaet(
                            nutzer,
                            AktivitaetsTyp.LOGIN,
                            "Anmeldung als " + getNutzerIdentifikation(nutzer),
                            details,
                            true,
                            null,
                            null
                    );
                    
                    log.debug("Erfolgreiche Anmeldung protokolliert für Nutzer: {}", username);
                });
            } catch (Exception e) {
                log.error("Fehler beim Protokollieren der erfolgreichen Anmeldung für {}: {}", 
                        username, e.getMessage(), e);
            }
        } else {
            log.warn("Authentication Principal ist kein UserDetails: {}", 
                    principal != null ? principal.getClass().getName() : "null");
        }
    }
    
    /**
     * Hilfsmethode zum Ermitteln einer lesbaren Identifikation eines Nutzers.
     *
     * @param nutzer Der Nutzer, dessen Identifikation ermittelt werden soll
     * @return Eine lesbare Identifikation des Nutzers (Name oder E-Mail)
     */
    private String getNutzerIdentifikation(NutzerDTO nutzer) {
        if (nutzer == null) {
            return "Unbekannt";
        }
        
        if (nutzer.getVorname() != null && !nutzer.getVorname().isEmpty() 
                && nutzer.getNachname() != null && !nutzer.getNachname().isEmpty()) {
            return nutzer.getVorname() + " " + nutzer.getNachname();
        } else if (nutzer.getEmail() != null && !nutzer.getEmail().isEmpty()) {
            return nutzer.getEmail();
        } else {
            return "Nutzer ID: " + nutzer.getId();
        }
    }
}
