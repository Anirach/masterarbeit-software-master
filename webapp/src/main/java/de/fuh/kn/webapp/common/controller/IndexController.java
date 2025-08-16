package de.fuh.kn.webapp.common.controller;

import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller für die Hauptseiten der Anwendung.
 */
@Controller
public class IndexController {

    private final NutzerService nutzerService;

    /**
     * Konstruktor mit Dependency Injection des NutzerService.
     *
     * @param nutzerService Der Service für die Verwaltung von Nutzern.
     */
    public IndexController(NutzerService nutzerService) {
        this.nutzerService = nutzerService;
    }

    /**
     * Leitet zur passenden Seite basierend auf dem Authentifizierungsstatus weiter.
     * Nicht angemeldete Nutzer werden zur Login-Seite weitergeleitet, 
     * angemeldete Nutzer zu ihrer rollenspezifischen Startseite.
     *
     * @return Die passende Weiterleitung basierend auf dem Authentifizierungsstatus
     */
    @GetMapping("/")
    public String handleRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        
        // Nicht angemeldete Nutzer zur Login-Seite weiterleiten
        return "redirect:/login";
    }
    
    /**
     * Zeigt die Informationsseite an.
     *
     * @return Der Name der Template-Datei für die Informationsseite.
     */
    @GetMapping("/information")
    public String showInformation() {
        return "index";
    }

    /**
     * Zeigt das Dashboard für eingeloggte Studierende an.
     *
     * @return Die passende Weiterleitung basierend auf dem Nutzertyp
     */
    @GetMapping("/dashboard")
    public String handleDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object authDetails = auth.getPrincipal();

            if(authDetails instanceof KursbetreuerUserDetails){
                return "redirect:/kursbetreuer/dashboard";
            }else if(authDetails instanceof StudentUserDetails){
                return "redirect:/student/dashboard";
            }

        }

        // Nicht angemeldete Nutzer zur Login-Seite weiterleiten
        return "redirect:/login";
    }
}
