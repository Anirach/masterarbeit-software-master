package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller für HTMX-basierte Operationen an Kursbelegungen.
 * Ermöglicht insbesondere die Suche und Filterung in der Belegungstabelle.
 */
@Controller
@RequestMapping("/kursbetreuer/htmx/belegungen")
@Slf4j
public class BelegungenHtmxController {

    private final BelegungService belegungService;
    private final KursService kursService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param belegungService Service für die Verwaltung von Belegungen
     * @param kursService Service für die Verwaltung von Kursen
     */
    @Autowired
    public BelegungenHtmxController(BelegungService belegungService, 
                                    KursService kursService) {
        this.belegungService = belegungService;
        this.kursService = kursService;
    }

    /**
     * Sucht und filtert Belegungen eines Kurses basierend auf Suchbegriff und Filteroption "nur aktive".
     * Gibt das aktualisierte Tabellen-Fragment zurück.
     *
     * @param kursId Die ID des Kurses
     * @param belegungSearch Der Suchbegriff (Matrikelnummer oder Name)
     * @param nurAktive Flag, ob nur aktive Belegungen angezeigt werden sollen
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit den gefilterten Belegungen
     */
    @PostMapping("/search/{kursId}")
    public String searchBelegungen(
            @PathVariable("kursId") Long kursId,
            @RequestParam(required = false) String belegungSearch,
            @RequestParam(required = false) Boolean nurAktive,
            Model model) {
        try {
            // Kurs über den KursService laden
            KursDTO kursDTO = kursService.getKursById(kursId);
            if (kursDTO == null) {
                throw new NoSuchElementException("Kurs mit ID " + kursId + " nicht gefunden");
            }
            
            // Belegungen laden über den BelegungService
            List<BelegungDTO> alleBelegungen = belegungService.getEnrollmentsByKursWithProgress(kursDTO);
            
            // Filtern nach Aktivität, falls gewünscht
            List<BelegungDTO> gefiltert = alleBelegungen;
            if (Boolean.TRUE.equals(nurAktive)) {
                gefiltert = alleBelegungen.stream()
                        .filter(BelegungDTO::isAktiv)
                        .collect(Collectors.toList());
            }
            
            // Filtern nach Suchbegriff, falls vorhanden
            if (belegungSearch != null && !belegungSearch.trim().isEmpty()) {
                String search = belegungSearch.toLowerCase().trim();
                gefiltert = gefiltert.stream()
                        .filter(b -> 
                            (b.getMatrikelnummer() != null && b.getMatrikelnummer().toLowerCase().contains(search)) ||
                            (b.getStudentName() != null && b.getStudentName().toLowerCase().contains(search)) ||
                            (search.equals("aktiv") && b.isAktiv()) ||
                            (search.equals("inaktiv") && !b.isAktiv())
                        )
                        .collect(Collectors.toList());
            }
            
            // Gefilterte Belegungen zum Model hinzufügen
            model.addAttribute("belegungen", gefiltert);
            model.addAttribute("kursId", kursId);
            
            // Fragment mit der gefilterten Tabelle zurückgeben
            return "fragments/kursbetreuer/belegungen-table :: .belegungen-table";
        } catch (Exception e) {
            // Fehlerfall: Leere Tabelle mit Fehlermeldung zurückgeben
            model.addAttribute("belegungen", List.of());
            model.addAttribute("kursId", kursId);
            model.addAttribute("errorMessage", "Fehler bei der Suche: " + e.getMessage());

            log.error("Fehler bei der Suche in der Belegungstabelle. kursId={}, belegungSearch={}", kursId, belegungSearch, e);
            
            return "fragments/kursbetreuer/belegungen-table :: .belegungen-table";
        }
    }
}