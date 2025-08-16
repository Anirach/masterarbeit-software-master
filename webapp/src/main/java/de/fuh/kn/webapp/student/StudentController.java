package de.fuh.kn.webapp.student;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller für studentenbezogene Funktionen wie Dashboard, Aufgabenbearbeitung und Lernfortschrittsverfolgung.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final AktivitaetsService aktivitaetsService;
    private final NutzerService nutzerService;
    private final BelegungService belegungService;
    private final KursService kursService;
    private final FortschrittService fortschrittService;

    public StudentController(AktivitaetsService aktivitaetsService,
                            NutzerService nutzerService,
                            BelegungService belegungService,
                            KursService kursService,
                            FortschrittService fortschrittService) {
        this.aktivitaetsService = aktivitaetsService;
        this.nutzerService = nutzerService;
        this.belegungService = belegungService;
        this.kursService = kursService;
        this.fortschrittService = fortschrittService;
    }

    /**
     * Zeigt das Dashboard für Studierende an.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für das Studenten-Dashboard.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Nutzer-Informationen laden
        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        StudentDTO student = nutzerService.getStudentById(nutzer.getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Kursbelegungen des Studierenden laden
        List<BelegungDTO> alleBelegungen = belegungService.getAllEnrollmentsByStudent(student);
        List<BelegungDTO> aktiveBelegungen = belegungService.getActiveEnrollmentsByStudent(student);

        // Kurse aus den Belegungen extrahieren
        List<KursDTO> alleKurse = alleBelegungen.stream()
                .map(belegung -> {
                    KursDTO kurs = kursService.getKursById(belegung.getKursId());
                    if (kurs == null) {
                        throw new IllegalStateException("Kurs mit ID " + belegung.getKursId() + " nicht gefunden");
                    }
                    return kurs;
                })
                .collect(Collectors.toList());

        List<KursDTO> aktiveKurse = aktiveBelegungen.stream()
                .map(belegung -> {
                    KursDTO kurs = kursService.getKursById(belegung.getKursId());
                    if (kurs == null) {
                        throw new IllegalStateException("Kurs mit ID " + belegung.getKursId() + " nicht gefunden");
                    }
                    return kurs;
                })
                .collect(Collectors.toList());

        // Fortschrittsdaten berechnen
        List<KursFortschrittDTO> aktiveFortschritte = fortschrittService.berechneFortschrittFuerKurse(student, aktiveKurse, true);

        // Collection mit aktiven Kurs-IDs erstellen für den Filter
        Set<Long> aktiveKursIds = aktiveKurse.stream()
                .map(KursDTO::getId)
                .collect(Collectors.toSet());

        // Inaktive Kurse filtern anhand ihrer ID
        List<KursFortschrittDTO> inaktiveFortschritte = fortschrittService.berechneFortschrittFuerKurse(
                student,
                alleKurse.stream()
                        .filter(kurs -> !aktiveKursIds.contains(kurs.getId()))
                        .collect(Collectors.toList()),
                false);

        // Anzahl gelöster Aufgaben
        Long anzahlGeloesterAufgaben = fortschrittService.zaehleSolvedAssignments(student.getId());

        // Aktivitäten des Studenten laden
        Page<AktivitaetDTO> aktivitaetDTOS = aktivitaetsService.findeAktivitaetenFuerNutzerPaged(nutzer, 0, 10);

        // Daten an die View übergeben
        model.addAttribute("aktivitaeten", aktivitaetDTOS);
        model.addAttribute("nutzer", nutzer);
        model.addAttribute("aktiveFortschritte", aktiveFortschritte);
        model.addAttribute("inaktiveFortschritte", inaktiveFortschritte);
        model.addAttribute("anzahlBelegteKurse", alleKurse.size());
        model.addAttribute("anzahlAktiveKurse", aktiveKurse.size());
        model.addAttribute("anzahlGeloesterAufgaben", anzahlGeloesterAufgaben);

        return "student/dashboard";
    }

}
