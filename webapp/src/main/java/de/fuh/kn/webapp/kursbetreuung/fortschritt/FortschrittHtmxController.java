package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HTMX-Controller für dynamische Updates der Fortschrittsübersicht.
 * 
 * Dieser Controller behandelt AJAX-Anfragen von HTMX für die dynamische
 * Aktualisierung der Fortschrittstabelle ohne vollständigen Seitenreload.
 * Er wird hauptsächlich für Filter- und Suchoperationen verwendet.
 */
@Slf4j
@Controller
@RequestMapping("/kursbetreuer/fortschritt/htmx")
public class FortschrittHtmxController {

    private final KursService kursService;
    private final FortschrittOverviewService fortschrittOverviewService;

    public FortschrittHtmxController(KursService kursService, FortschrittOverviewService fortschrittOverviewService) {
        this.kursService = kursService;
        this.fortschrittOverviewService = fortschrittOverviewService;
    }

    /**
     * Liefert das Fortschrittstabellen-Fragment für HTMX-Updates.
     * 
     * Diese Methode wird von HTMX aufgerufen, wenn Filterkriterien geändert werden
     * oder eine andere Seite angefordert wird. Sie gibt nur das Tabellen-Fragment
     * zurück, nicht die komplette Seite.
     * 
     * @param kursId Die ID des Kurses
     * @param page Die aktuelle Seitennummer
     * @param size Die Anzahl der Einträge pro Seite
     * @param search Suchbegriff für Studentennamen oder E-Mail
     * @param nurAktiv Ob nur aktive Belegungen angezeigt werden sollen
     * @param model Das Spring MVC Model
     * @return Der Name des Tabellen-Fragments
     */
    @GetMapping("/kurs/{kursId}")
    public String fortschrittTableFragment(
            @PathVariable Long kursId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "true") boolean nurAktiv,
            Model model) {
        
        // Kurs-Daten laden
        KursDTO kurs = kursService.getKursById(kursId);

        // Paginierung und Fortschrittsdaten abrufen
        Pageable pageable = PageRequest.of(page, size);
        Page<FortschrittOverviewDTO> fortschrittsPage = fortschrittOverviewService
                .getFortschrittOverviewForKurs(kurs, search, nurAktiv, pageable);

        // Model-Attribute für das Fragment setzen
        model.addAttribute("fortschrittsPage", fortschrittsPage);
        model.addAttribute("kursId", kursId);

        // Nur das Tabellen-Fragment zurückgeben
        return "fragments/kursbetreuer/fortschritt-table :: fortschritt-table";
    }
}