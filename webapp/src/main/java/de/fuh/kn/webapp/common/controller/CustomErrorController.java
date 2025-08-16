package de.fuh.kn.webapp.common.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Benutzerdefinierter Error-Controller, der die Standard-Whitelabel-Fehlerseite ersetzt
 * und stattdessen eine angepasste Fehlerseite mit dem Layout der Anwendung anzeigt.
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Behandelt alle Fehleranfragen und leitet zur benutzerdefinierten Fehlerseite weiter.
     * 
     * @param request Die HTTP-Anfrage, die den Fehler enthält
     * @param model Das Modell für die Thymeleaf-Vorlage
     * @return Der Name der Fehlerseiten-Vorlage
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Status-Code holen
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        // Status-Code ins Modell einfügen
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);
            
            // Spezifische Fehlermeldungen basierend auf Status-Code
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Seite nicht gefunden");
                model.addAttribute("message", "Die angeforderte Seite existiert nicht.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("error", "Zugriff verweigert");
                model.addAttribute("message", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Interner Serverfehler");
                model.addAttribute("message", "Bei der Verarbeitung Ihrer Anfrage ist ein Fehler aufgetreten.");
            } else {
                model.addAttribute("error", "Fehler " + statusCode);
                model.addAttribute("message", errorMessage != null ? errorMessage.toString() : "Ein unerwarteter Fehler ist aufgetreten.");
            }
        } else {
            model.addAttribute("error", "Unbekannter Fehler");
            model.addAttribute("message", "Ein unerwarteter Fehler ist aufgetreten.");
        }
        
        return "error";
    }
}
