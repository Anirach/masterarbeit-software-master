package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
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
 * Controller für die Fortschrittsübersicht in der Kursbetreuung.
 * 
 * Dieser Controller stellt Endpunkte zur Verfügung, um den Lernfortschritt
 * aller Studierenden in einem Kurs zu überwachen. Er bietet eine Übersichtsseite
 * mit aggregierten Daten sowie eine Detailansicht pro Student.
 */
@Slf4j
@Controller
@RequestMapping("/kursbetreuer/fortschritt")
public class KursbetreuerFortschrittController {

    private final KursService kursService;
    private final BelegungService belegungService;
    private final FortschrittOverviewService fortschrittOverviewService;

    public KursbetreuerFortschrittController(KursService kursService, BelegungService belegungService, FortschrittOverviewService fortschrittOverviewService) {
        this.kursService = kursService;
        this.belegungService = belegungService;
        this.fortschrittOverviewService = fortschrittOverviewService;
    }

    /**
     * Zeigt die Fortschrittsübersicht für einen bestimmten Kurs an.
     * 
     * @param kursId Die ID des Kurses
     * @param page Die aktuelle Seitennummer (Standard: 0)
     * @param size Die Anzahl der Einträge pro Seite (Standard: 10)
     * @param search Suchbegriff für Studentennamen oder E-Mail (Standard: leer)
     * @param nurAktiv Ob nur aktive Belegungen angezeigt werden sollen (Standard: true)
     * @param model Das Spring MVC Model
     * @return Der Name der View-Template
     */
    @GetMapping("/kurs/{kursId}")
    public String fortschrittUebersicht(
            @PathVariable Long kursId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "true") boolean nurAktiv,
            Model model) {

        // Kurs-Daten laden
        KursDTO kurs = kursService.getKursById(kursId);

        // Paginierung vorbereiten
        Pageable pageable = PageRequest.of(page, size);
        
        // Fortschrittsdaten abrufen
        Page<FortschrittOverviewDTO> fortschrittsPage = fortschrittOverviewService
                .getFortschrittOverviewForKurs(kurs, search, nurAktiv, pageable);

        // Model-Attribute für die View setzen
        model.addAttribute("kurs", kurs);
        model.addAttribute("fortschrittsPage", fortschrittsPage);
        model.addAttribute("search", search);
        model.addAttribute("nurAktiv", nurAktiv);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "kursbetreuer/fortschritt/fortschritt-kurs";
    }

    /**
     * Zeigt die detaillierte Fortschrittsansicht für einen einzelnen Studenten an.
     * 
     * Diese Ansicht bietet eine umfassende Übersicht über den Lernfortschritt eines Studenten:
     * - Detaillierte Aufgabenübersicht mit allen Lösungsversuchen
     * - Bewertungen und Punkte pro Aufgabe
     * - Chat-Verlauf und KI-Interaktionen
     * - Detaillierte KI-Kostenaufstellung
     * 
     * @param belegungId Die ID der Belegung
     * @param model Das Spring MVC Model
     * @return Der Name der View-Template
     */
    @GetMapping("/student/{belegungId}")
    public String studentDetail(
            @PathVariable Long belegungId,
            Model model) {

        // Belegungsdaten laden
        BelegungDTO belegung = belegungService.getBelegungById(belegungId);

        // Detaillierte Fortschrittsansicht erstellen
        StudentDetailDTO studentDetail = fortschrittOverviewService
                .getStudentDetail(belegung.getStudentId(), belegung.getKursId());

        // Model-Attribute für die View setzen
        model.addAttribute("belegung", belegung);
        model.addAttribute("studentDetail", studentDetail);

        return "kursbetreuer/fortschritt/fortschritt-student-detail";
    }
}