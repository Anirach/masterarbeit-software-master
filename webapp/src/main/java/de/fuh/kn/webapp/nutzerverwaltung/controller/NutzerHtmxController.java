package de.fuh.kn.webapp.nutzerverwaltung.controller;

import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller für HTMX-basierte Operationen an der Nutzerverwaltung.
 * Ermöglicht insbesondere die Suche und Filterung in der Nutzertabellen.
 */
@Slf4j
@Controller
@RequestMapping("/kursbetreuer/htmx/nutzer")
public class NutzerHtmxController {

    private final NutzerService nutzerService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param nutzerService Service für die Verwaltung von Nutzern
     */
    @Autowired
    public NutzerHtmxController(NutzerService nutzerService) {
        this.nutzerService = nutzerService;
    }

    /**
     * Sucht und filtert Studenten basierend auf einem Suchbegriff.
     * Gibt das aktualisierte Tabellen-Fragment zurück.
     *
     * @param studentSearch Der Suchbegriff (Matrikelnummer, Name, Email)
     * @param nurRegistriert Flag, ob nur registrierte Studenten angezeigt werden sollen
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit den gefilterten Studenten
     */
    @PostMapping("/search/studenten")
    public String searchStudenten(
            @RequestParam(required = false) String studentSearch,
            @RequestParam(required = false) Boolean nurRegistriert,
            Model model) {
        try {
            // Alle Studenten laden
            List<StudentDTO> alleStudenten = nutzerService.getAlleStudenten();
            
            // Filtern nach Registrierungsstatus, falls gewünscht
            List<StudentDTO> gefiltert = alleStudenten;
            if (Boolean.TRUE.equals(nurRegistriert)) {
                gefiltert = alleStudenten.stream()
                        .filter(StudentDTO::getIstRegistriert)
                        .collect(Collectors.toList());
            }
            
            // Filtern nach Suchbegriff, falls vorhanden
            if (studentSearch != null && !studentSearch.trim().isEmpty()) {
                String search = studentSearch.toLowerCase().trim();
                gefiltert = gefiltert.stream()
                        .filter(s -> 
                            (s.getMatrikelnummer() != null && s.getMatrikelnummer().toLowerCase().contains(search)) ||
                            (s.getVorname() != null && s.getVorname().toLowerCase().contains(search)) ||
                            (s.getNachname() != null && s.getNachname().toLowerCase().contains(search)) ||
                            (s.getEmail() != null && s.getEmail().toLowerCase().contains(search)) ||
                            (search.equals("registriert") && s.getIstRegistriert()) ||
                            (search.equals("nicht registriert") && !s.getIstRegistriert())
                        )
                        .collect(Collectors.toList());
            }
            
            // Gefilterte Studenten zum Model hinzufügen
            model.addAttribute("studenten", gefiltert);
            
            // Fragment mit der gefilterten Tabelle zurückgeben
            return "fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table";
        } catch (Exception e) {
            // Fehlerfall: Leere Tabelle mit Fehlermeldung zurückgeben
            model.addAttribute("studenten", List.of());
            model.addAttribute("errorMessage", "Fehler bei der Suche: " + e.getMessage());

            log.error("Fehler bei der Suche in der Studenten-Tabelle", e);
            
            return "fragments/kursbetreuer/nutzerverwaltung/studenten-table :: .studenten-table";
        }
    }

    /**
     * Sucht und filtert Kursbetreuer basierend auf einem Suchbegriff.
     * Gibt das aktualisierte Tabellen-Fragment zurück.
     *
     * @param betreuerSearch Der Suchbegriff (Name, Email)
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit den gefilterten Kursbetreuern
     */
    @PostMapping("/search/kursbetreuer")
    public String searchKursbetreuer(
            @RequestParam(required = false) String betreuerSearch,
            Model model) {
        try {
            // Alle Kursbetreuer laden
            List<KursbetreuerDTO> alleKursbetreuer = nutzerService.getAlleKursbetreuer();
            
            // Filtern nach Suchbegriff, falls vorhanden
            List<KursbetreuerDTO> gefiltert = alleKursbetreuer;
            if (betreuerSearch != null && !betreuerSearch.trim().isEmpty()) {
                String search = betreuerSearch.toLowerCase().trim();
                gefiltert = gefiltert.stream()
                        .filter(k -> 
                            (k.getVorname() != null && k.getVorname().toLowerCase().contains(search)) ||
                            (k.getNachname() != null && k.getNachname().toLowerCase().contains(search)) ||
                            (k.getEmail() != null && k.getEmail().toLowerCase().contains(search))
                        )
                        .collect(Collectors.toList());
            }
            
            // Gefilterte Kursbetreuer zum Model hinzufügen
            model.addAttribute("kursbetreuer", gefiltert);
            
            // Fragment mit der gefilterten Tabelle zurückgeben
            return "fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table";
        } catch (Exception e) {
            // Fehlerfall: Leere Tabelle mit Fehlermeldung zurückgeben
            model.addAttribute("kursbetreuer", List.of());
            model.addAttribute("errorMessage", "Fehler bei der Suche: " + e.getMessage());

            log.error("Fehler bei der Suche in der Kursbetreuer-Tabelle", e);

            return "fragments/kursbetreuer/nutzerverwaltung/kursbetreuer-table :: .kursbetreuer-table";
        }
    }
}