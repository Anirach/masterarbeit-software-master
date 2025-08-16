package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller für HTMX-basierte Operationen an Kurseinheiten.
 * Dieser Controller stellt spezielle Endpunkte bereit, die insbesondere für
 * HTMX-Anfragen und partielle Seitenaktualisierungen in Bezug auf Kurseinheiten optimiert sind.
 */
@Controller
@RequestMapping("/kursbetreuer/htmx")
@Slf4j
public class KursbetreuerKurseinheitHtmxController {

    private final KursService kursService;
    private final KurseinheitService kurseinheitService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     */
    @Autowired
    public KursbetreuerKurseinheitHtmxController(
            KursService kursService,
            KurseinheitService kurseinheitService) {
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
    }

    /**
     * Aktualisiert die Reihenfolge der Kurseinheiten eines Kurses per Drag & Drop.
     * Diese Methode nimmt eine Liste von Kurseinheit-IDs in der neuen Reihenfolge entgegen und aktualisiert die Datenbank.
     *
     * @param kursId Die ID des Kurses, dessen Kurseinheiten neu sortiert werden
     * @param ids Liste der Kurseinheit-IDs in der neuen Reihenfolge
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit der aktualisierten Kurseinheiten-Tabelle und einer Erfolgsmeldung
     */
    @PostMapping("/kurse/{kursId}/kurseinheiten/reorder")
    public String reorderKurseinheiten(
            @PathVariable("kursId") Long kursId,
            @RequestParam List<Long> ids,
            Model model) {
        try {
            // Reihenfolge aktualisieren
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("Keine Kurseinheit-IDs erhalten");
            }
            
            kurseinheitService.aktualisiereKurseinheitenReihenfolge(kursId, ids);
            
            // Kurs mit aktualisierten Kurseinheiten laden
            KursDTO kurs = kursService.getKursByIdMitKurseinheiten(kursId);
            model.addAttribute("kurs", kurs);
            
            // Erfolgsmeldung setzen
            model.addAttribute("successMessage", "Die Reihenfolge der Kurseinheiten wurde erfolgreich aktualisiert.");
            
            // Kurseinheiten-Tabellen-Fragment zurückgeben
            return "fragments/kursbetreuer/kurseinheiten-table :: kurseinheiten-table";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler bei der Aktualisierung der Reihenfolge: " + e.getMessage());

            log.error("Fehler beim Umsortieren von Kurseinheiten", e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }
}