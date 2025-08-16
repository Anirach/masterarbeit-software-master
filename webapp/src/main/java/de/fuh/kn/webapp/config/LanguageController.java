package de.fuh.kn.webapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller für das Wechseln der Sprache.
 * Leitet nach dem Sprachwechsel auf die vorherige Seite zurück.
 */
@Controller
public class LanguageController {

    /**
     * Wechselt die Sprache und leitet auf die vorherige Seite zurück.
     * 
     * @param lang die gewünschte Sprache (de oder en)
     * @param request der HTTP-Request, um die Referrer-URL zu ermitteln
     * @return Redirect zur vorherigen Seite oder zur Startseite
     */
    @GetMapping("/change-language")
    public String changeLanguage(@RequestParam("lang") String lang, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        
        // Wenn keine Referrer-URL vorhanden ist, zur Startseite zurück
        if (referer == null || referer.isEmpty()) {
            return "redirect:/?lang=" + lang;
        }
        
        // Prüfen, ob die Referrer-URL bereits einen lang-Parameter enthält
        String separator = referer.contains("?") ? "&" : "?";
        
        // Bestehende lang-Parameter entfernen
        String cleanReferer = referer.replaceAll("[?&]lang=[^&]*", "");
        
        // Neuen lang-Parameter hinzufügen
        return "redirect:" + cleanReferer + separator + "lang=" + lang;
    }
}
