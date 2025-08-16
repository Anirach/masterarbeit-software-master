package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller für die Verwaltung von Kurseinheiten durch Kursbetreuer.
 * Dieser Controller stellt Funktionen zum Anzeigen, Erstellen, Bearbeiten und Löschen von Kurseinheiten bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerKurseinheitController {

    private final KurseinheitService kurseinheitService;
    private final KursService kursService;
    private final AufgabeService aufgabeService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     * @param kursService Der Service für die Verwaltung von Kursen
     */
    @Autowired
    public KursbetreuerKurseinheitController(KurseinheitService kurseinheitService, KursService kursService, AufgabeService aufgabeService) {
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;
        this.aufgabeService = aufgabeService;
    }

    /**
     * Zeigt das Formular zum Erstellen einer neuen Kurseinheit an.
     *
     * @param kursId Die ID des Kurses, zu dem die Kurseinheit hinzugefügt werden soll
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Kurseinheit-Formular
     */
    @GetMapping("/kurse/{kursId}/kurseinheiten/neu")
    public String showCreateKurseinheitForm(@PathVariable("kursId") Long kursId, Model model) {
        // Prüfen, ob der Kurs existiert
        KursDTO kurs = kursService.getKursById(kursId);
        if (kurs == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        // Neue Kurseinheit erstellen und mit der Kurs-ID vorausfüllen
        KurseinheitDTO kurseinheitDTO = new KurseinheitDTO();
        kurseinheitDTO.setKursId(kursId);
        
        // Bestimme die nächste freie Reihenfolgennummer
        int naechsteReihenfolge = 1;
        if (kurs.getKurseinheiten() != null && !kurs.getKurseinheiten().isEmpty()) {
            naechsteReihenfolge = kurs.getKurseinheiten().size() + 1;
        }
        kurseinheitDTO.setReihenfolge(naechsteReihenfolge);
        
        model.addAttribute("kurseinheit", kurseinheitDTO);
        model.addAttribute("kursName", kurs.getName());
        model.addAttribute("isNew", true);
        
        return "kursbetreuer/kurseinheit/kurseinheit-form";
    }

    /**
     * Zeigt das Formular zum Bearbeiten einer bestehenden Kurseinheit an.
     *
     * @param id Die ID der zu bearbeitenden Kurseinheit
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Kurseinheit-Formular
     */
    @GetMapping("/kurseinheiten/{id}/bearbeiten")
    public String showEditKurseinheitForm(@PathVariable("id") Long id, Model model) {
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(id);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        String kursName = kurseinheitService.getKursNameByKurseinheitId(id);
        
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kursName", kursName);
        model.addAttribute("isNew", false);
        
        return "kursbetreuer/kurseinheit/kurseinheit-form";
    }

    /**
     * Verarbeitet das Speichern einer neuen oder bearbeiteten Kurseinheit.
     *
     * @param kurseinheit Das KurseinheitDTO mit den eingegebenen Daten
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kursdetailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURSEINHEIT_BEARBEITEN,
            beschreibung = "Kurseinheit \"{0}\" gespeichert"
    )
    @PostMapping("/kurseinheiten/speichern")
    public String saveKurseinheit(
            @ModelAttribute("kurseinheit") KurseinheitDTO kurseinheit,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (kurseinheit.getId() == null) {
            // Neue Kurseinheit erstellen
            KurseinheitDTO erstellteKurseinheit = kurseinheitService.erstelleKurseinheit(kurseinheit);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Die Kurseinheit \"" + erstellteKurseinheit.getName() + "\" wurde erfolgreich erstellt.");
            model.addAttribute("kurseinheit", erstellteKurseinheit);
            return "redirect:/kursbetreuer/kurse/" + kurseinheit.getKursId();
        } else {
            // Bestehende Kurseinheit aktualisieren
            KurseinheitDTO aktualiserteKurseinheit = kurseinheitService.aktualisiereKurseinheit(kurseinheit);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Die Kurseinheit \"" + aktualiserteKurseinheit.getName() + "\" wurde erfolgreich aktualisiert.");
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheit.getId();
        }
    }

    /**
     * Zeigt die Bestätigungsseite zum Löschen einer Kurseinheit an.
     *
     * @param id Die ID der zu löschenden Kurseinheit
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Löschbestätigung
     */
    @GetMapping("/kurseinheiten/{id}/loeschen")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(id);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        String kursName = kurseinheitService.getKursNameByKurseinheitId(id);
        
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kursName", kursName);
        
        return "kursbetreuer/kurseinheit/kurseinheit-loeschen";
    }

    /**
     * Löscht eine Kurseinheit anhand ihrer ID nach Bestätigung.
     *
     * @param id Die ID der zu löschenden Kurseinheit
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kursdetailseite
     */
    @ProtokolliereAktivitaet(aktivitaetsTyp = AktivitaetsTyp.KURSEINHEIT_LOESCHEN)
    @PostMapping("/kurseinheiten/{id}/loeschen")
    public String deleteKurseinheitPost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Model model) {
        // Kurseinheit-Namen und Kurs-ID vor dem Löschen abrufen
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(id);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        String kurseinheitName = kurseinheit.getName();
        Long kursId = kurseinheit.getKursId();
        
        // Kurseinheit löschen
        kurseinheitService.loescheKurseinheit(id);
        
        // Erfolgsmeldung als Flash-Attribut hinzufügen
        redirectAttributes.addFlashAttribute("successMessage", 
            "Die Kurseinheit \"" + kurseinheitName + "\" wurde erfolgreich gelöscht.");
        model.addAttribute("kurseinheit", kurseinheit);
            
        return "redirect:/kursbetreuer/kurse/" + kursId;
    }
    
    /**
     * Zeigt die Detailansicht einer Kurseinheit an.
     *
     * @param id Die ID der anzuzeigenden Kurseinheit
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Kurseinheit-Details
     */
    @GetMapping("/kurseinheiten/{id}")
    public String showKurseinheitDetails(@PathVariable("id") Long id, Model model) {
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(id);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        // Zugehörigen Kurs laden
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        if (kurs == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        // Kursmaterialien der Kurseinheit laden
        List<KursMaterialDTO> kursMaterialien = kurseinheitService.getKursMaterialienByKurseinheitId(id);

        //Aufgaben der Kurseinheit laden
        List<AufgabeDto> aufgaben = aufgabeService.getAufgabenByKurseinheitId(id);

        // Daten an das Model übergeben
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kurseinheitId", kurseinheit.getId());
        model.addAttribute("kurs", kurs);
        model.addAttribute("kursMaterialien", kursMaterialien);
        model.addAttribute("aufgaben", aufgaben);
        
        return "kursbetreuer/kurseinheit/kurseinheit-details";
    }
}
