package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursbetreuung.dashboard.DashboardStatisticsDTO;
import de.fuh.kn.webapp.kursbetreuung.dashboard.DashboardStatisticsService;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller für die Verwaltung von Kursen durch Kursbetreuer.
 * Dieser Controller stellt Funktionen zum Anzeigen, Erstellen, Bearbeiten und Löschen von Kursen bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerKursController {

    private final KursService kursService;
    private final AktivitaetsService aktivitaetsService;
    private final NutzerService nutzerService;
    private final DashboardStatisticsService dashboardStatisticsService;

    /**
     * Konstruktor mit Dependency Injection des KursService.
     *
     * @param kursService Der Service für die Verwaltung von Kursen.
     */
    @Autowired
    public KursbetreuerKursController(KursService kursService, AktivitaetsService aktivitaetsService, 
                                     NutzerService nutzerService, DashboardStatisticsService dashboardStatisticsService) {
        this.kursService = kursService;
        this.aktivitaetsService = aktivitaetsService;
        this.nutzerService = nutzerService;
        this.dashboardStatisticsService = dashboardStatisticsService;
    }

    /**
     * Zeigt das Dashboard für Kursbetreuer an mit einer Liste aller verfügbaren Kurse.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für das Dashboard.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        Page<AktivitaetDTO> alleAktivitaeten = aktivitaetsService.getAlleAktivitaeten(0, 10);
        model.addAttribute("aktivitaeten", alleAktivitaeten);

        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        model.addAttribute("nutzer", nutzer);
        
        // Dashboard-Statistiken hinzufügen
        DashboardStatisticsDTO statistics = dashboardStatisticsService.getDashboardStatistics();
        model.addAttribute("statistics", statistics);

        return "kursbetreuer/dashboard";
    }

    /**
     * Zeigt die Detailansicht eines Kurses an.
     *
     * @param id Die ID des Kurses.
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Kursdetailansicht.
     */
    @GetMapping("/kurse/{id}")
    public String showKursDetails(@PathVariable("id") Long id, Model model) {
        KursDTO kurs = kursService.getKursByIdMitKurseinheiten(id);
        model.addAttribute("kurs", kurs);
        return "kursbetreuer/kurs/kurs-details";
    }

    /**
     * Zeigt das Formular zum Erstellen eines neuen Kurses an.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für das Kursformular.
     */
    @GetMapping("/kurse/neu")
    public String showCreateKursForm(Model model) {
        model.addAttribute("kurs", new KursDTO());
        model.addAttribute("isNew", true);
        return "kursbetreuer/kurs/kurs-form";
    }

    /**
     * Zeigt das Formular zum Bearbeiten eines bestehenden Kurses an.
     *
     * @param id Die ID des zu bearbeitenden Kurses.
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für das Kursformular.
     */
    @GetMapping("/kurse/{id}/bearbeiten")
    public String showEditKursForm(@PathVariable("id") Long id, Model model) {
        KursDTO kurs = kursService.getKursById(id);
        model.addAttribute("kurs", kurs);
        model.addAttribute("isNew", false);
        return "kursbetreuer/kurs/kurs-form";
    }

    /**
     * Verarbeitet das Speichern eines neuen oder bearbeiteten Kurses.
     *
     * @param kurs Das KursDTO mit den eingegebenen Kursdaten.
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen.
     * @return Eine Weiterleitung zur Kursübersicht.
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURS_BEARBEITEN,
            beschreibung = "Kurs \"{0}\" gespeichert",
            mitParametern = true)
    @PostMapping("/kurse/speichern")
    public String saveKurs(
            @ModelAttribute("kurs") KursDTO kurs,
            Model model,
            RedirectAttributes redirectAttributes) {
        KursDTO gespeicherterKurs;
        boolean isNew = kurs.getId() == null;
        
        if (isNew) {
            gespeicherterKurs = kursService.erstelleKurs(kurs);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Der Kurs \"" + gespeicherterKurs.getName() + "\" wurde erfolgreich erstellt.");
        } else {
            gespeicherterKurs = kursService.aktualisiereKurs(kurs);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Der Kurs \"" + gespeicherterKurs.getName() + "\" wurde erfolgreich aktualisiert.");
        }
        model.addAttribute("kurs", gespeicherterKurs);
        return "redirect:/kursbetreuer/kurse/" + gespeicherterKurs.getId();
    }

    /**
     * Zeigt die Bestätigungsseite zum Löschen eines Kurses an.
     *
     * @param id Die ID des zu löschenden Kurses.
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Löschbestätigung.
     */
    @GetMapping("/kurse/{id}/loeschen")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        KursDTO kurs = kursService.getKursById(id);
        model.addAttribute("kurs", kurs);
        return "kursbetreuer/kurs/kurs-loeschen";
    }

    /**
     * Löscht einen Kurs anhand seiner ID nach Bestätigung.
     *
     * @param id Die ID des zu löschenden Kurses.
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen.
     * @return Eine Weiterleitung zur Kursübersicht.
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURS_LOESCHEN,
            mitParametern = false)
    @PostMapping("/kurse/{id}/loeschen")
    public String deleteKursPost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Model model) {
        // Kurs-Namen vor dem Löschen abrufen
        KursDTO kurs = kursService.getKursById(id);
        String kursName = kurs != null ? kurs.getName() : "Kurs";

        // Kurs löschen
        kursService.loescheKurs(id);

        // Erfolgsmeldung als Flash-Attribut hinzufügen
        redirectAttributes.addFlashAttribute("successMessage", 
            "Der Kurs \"" + kursName + "\" wurde erfolgreich gelöscht.");
        model.addAttribute("kurs", kurs);
            
        return "redirect:/kursbetreuer/kursverwaltung";
    }

    /**
     * Zeigt die Kursverwaltungsübersicht an.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Kursverwaltung.
     */
    @GetMapping("/kursverwaltung")
    public String showKursverwaltung(Model model) {
        List<KursDTO> kurse = kursService.getAlleKurse();
        model.addAttribute("kurse", kurse);
        return "kursbetreuer/kurs/kursverwaltung";
    }
}
