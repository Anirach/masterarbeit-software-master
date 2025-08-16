package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.uebung.dto.AufgabeFortschrittDto;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller für studentenbezogene Kurs-Funktionen wie Kursansicht, Aufgabenbearbeitung.
 */
@Controller
@RequestMapping("/student/kurs")
public class StudentKursController {

    private final KursService kursService;
    private final NutzerService nutzerService;
    private final FortschrittService fortschrittService;
    private final LoesungsversuchService loesungsversuchService;

    @Autowired
    public StudentKursController(
            KursService kursService,
            NutzerService nutzerService,
            FortschrittService fortschrittService, LoesungsversuchService loesungsversuchService) {
        this.kursService = kursService;
        this.nutzerService = nutzerService;
        this.fortschrittService = fortschrittService;
        this.loesungsversuchService = loesungsversuchService;
    }

    /**
     * Zeigt die Detailansicht eines Kurses für Studenten an.
     *
     * @param id    Die ID des Kurses.
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Kursdetailansicht.
     */
    @GetMapping("/{id}")
    public String showKursDetails(@PathVariable("id") Long id, Model model) {
        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Kurs mit Kurseinheiten laden
        KursDTO kurs = kursService.getKursByIdMitKurseinheiten(id);

        // Fortschritt für diesen Kurs berechnen
        KursFortschrittDTO fortschritt = fortschrittService.berechneFortschritt(studentDTO, kurs, true);

        // Fortschritt der Aufgaben für diesen Kurs ermitteln
        Map<Long, List<AufgabeFortschrittDto>> aufgabenFortschritteProKurseinheit =
                fortschrittService.erstelleAufgabenFortschritteProKurseinheitMap(kurs, studentDTO);

        // Bestimme welche Kurseinheiten erweitert werden sollen
        Set<Long> expandedKurseinheiten = bestimmeErweiterteKurseinheiten(kurs, aufgabenFortschritteProKurseinheit);

        // Daten an das View-Model übergeben
        model.addAttribute("kurs", kurs);
        model.addAttribute("fortschritt", fortschritt);
        model.addAttribute("aufgabenFortschritteProKurseinheit", aufgabenFortschritteProKurseinheit);
        model.addAttribute("expandedKurseinheiten", expandedKurseinheiten);
        model.addAttribute("student", studentDTO);

        return "student/kurs/kurs-details";
    }

    /**
     * Zeigt die Bestätigungsseite für das Zurücksetzen des Kursfortschritts an.
     *
     * @param id    Die ID des Kurses.
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Reset-Bestätigung.
     */
    @GetMapping("/{id}/reset")
    public String showKursResetConfirmation(@PathVariable("id") Long id, Model model) {
        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Kurs laden
        KursDTO kurs = kursService.getKursByIdMitKurseinheiten(id);

        // Daten an das View-Model übergeben
        model.addAttribute("kurs", kurs);
        model.addAttribute("student", studentDTO);

        return "student/kurs/kurs-reset";
    }

    /**
     * Führt das Zurücksetzen des Kursfortschritts für einen Studenten aus.
     *
     * @param id                Die ID des Kurses.
     * @param redirectAttributes Attribute für die Weiterleitung.
     * @return Weiterleitung zur Kursdetailseite.
     */
    @PostMapping("/{id}/reset")
    public String resetKursFortschritt(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Aktuell angemeldeten Studenten laden
            StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                    .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

            // Kurs laden für den Namen in der Erfolgsmendelung
            KursDTO kurs = kursService.getKursByIdMitKurseinheiten(id);

            // Fortschritt zurücksetzen
            loesungsversuchService.setzeKursLoesungsversucheZurueck(studentDTO.getId(), id);

            // Erfolgsmeldung hinzufügen
            redirectAttributes.addFlashAttribute("successMessage", 
                "Ihr Fortschritt im Kurs \"" + kurs.getName() + "\" wurde erfolgreich zurückgesetzt.");

        } catch (Exception e) {
            // Fehlermeldung hinzufügen
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Fehler beim Zurücksetzen des Kursfortschritts: " + e.getMessage());
        }

        return "redirect:/student/kurs/" + id;
    }

    /**
     * Bestimmt, welche Kurseinheiten erweitert angezeigt werden sollen.
     * Erweitert werden die Kurseinheit mit der ersten ungelösten Aufgabe und alle davor liegenden Kurseinheiten.
     *
     * @param kurs Der Kurs mit seinen Kurseinheiten
     * @param aufgabenFortschritteProKurseinheit Die Fortschritte aller Aufgaben pro Kurseinheit
     * @return Set der IDs der Kurseinheiten, die erweitert werden sollen
     */
    private Set<Long> bestimmeErweiterteKurseinheiten(KursDTO kurs, Map<Long, List<AufgabeFortschrittDto>> aufgabenFortschritteProKurseinheit) {
        Set<Long> expandedKurseinheiten = new HashSet<>();
        
        // Kurseinheiten nach Reihenfolge sortieren
        List<de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO> sortedKurseinheiten = kurs.getKurseinheiten().stream()
                .sorted(Comparator.comparing(de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO::getReihenfolge))
                .collect(Collectors.toList());
        
        Long kurseinheitMitErsterUngeloesterAufgabe = null;
        
        // Finde die Kurseinheit mit der ersten ungelösten Aufgabe
        for (de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO kurseinheit : sortedKurseinheiten) {
            List<AufgabeFortschrittDto> aufgabenFortschritte = aufgabenFortschritteProKurseinheit.get(kurseinheit.getId());
            
            if (aufgabenFortschritte != null && !aufgabenFortschritte.isEmpty()) {
                // Prüfe, ob es in dieser Kurseinheit ungelöste Aufgaben gibt
                boolean hatUngelosteAufgabe = aufgabenFortschritte.stream()
                        .anyMatch(fortschritt -> fortschritt.isFreigeschaltet() && 
                                (fortschritt.getAbgeschlosseneTeilaufgaben() < fortschritt.getAufgabe().getTeilaufgaben().size()));
                
                if (hatUngelosteAufgabe) {
                    kurseinheitMitErsterUngeloesterAufgabe = kurseinheit.getId();
                    break;
                }
            }
        }
        
        // Wenn eine Kurseinheit mit ungelöster Aufgabe gefunden wurde, alle bis einschließlich dieser erweitern
        if (kurseinheitMitErsterUngeloesterAufgabe != null) {
            for (de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO kurseinheit : sortedKurseinheiten) {
                expandedKurseinheiten.add(kurseinheit.getId());
                if (kurseinheit.getId().equals(kurseinheitMitErsterUngeloesterAufgabe)) {
                    break;
                }
            }
        } else {
            // Falls alle Aufgaben gelöst sind, erweitere die erste Kurseinheit
            if (!sortedKurseinheiten.isEmpty()) {
                expandedKurseinheiten.add(sortedKurseinheiten.get(0).getId());
            }
        }
        
        return expandedKurseinheiten;
    }
}