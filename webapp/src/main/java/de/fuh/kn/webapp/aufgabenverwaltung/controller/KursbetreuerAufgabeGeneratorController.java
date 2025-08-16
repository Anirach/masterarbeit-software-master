package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabenGeneratorService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller für die Generierung von Aufgaben basierend auf Kursmaterial.
 * Dieser Controller stellt Funktionen zum Generieren neuer Aufgaben für Kurseinheiten bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerAufgabeGeneratorController {

    private final KurseinheitService kurseinheitService;
    private final KursService kursService;
    private final AufgabeService aufgabeService;
    private final AufgabenGeneratorService aufgabenGeneratorService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param aufgabeService Der Service für die Verwaltung von Aufgaben
     * @param aufgabenGeneratorService Der Service für die Generierung von Aufgaben
     */
    @Autowired
    public KursbetreuerAufgabeGeneratorController(
            KurseinheitService kurseinheitService,
            KursService kursService,
            AufgabeService aufgabeService,
            AufgabenGeneratorService aufgabenGeneratorService) {
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;
        this.aufgabeService = aufgabeService;
        this.aufgabenGeneratorService = aufgabenGeneratorService;
    }

    /**
     * Zeigt das Formular zum Generieren von Aufgaben an.
     *
     * @param kurseinheitId Die ID der Kurseinheit, für die Aufgaben generiert werden sollen
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Generator-Formular
     */
    @GetMapping("/kurseinheiten/{kurseinheitId}/aufgaben/generator")
    public String zeigeGeneratorFormular(@PathVariable("kurseinheitId") Long kurseinheitId, Model model) {
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        List<AufgabeDto> aufgaben = aufgabeService.getAufgabenByKursId(kurs.getId());
        List<KurseinheitDTO> kurseinheiten = kurseinheitService.getKurseinheitenByKursId(kurs.getId());
        
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kurs", kurs);
        model.addAttribute("aufgaben", aufgaben);
        model.addAttribute("kurseinheiten", kurseinheiten);
        model.addAttribute("thema", "");
        model.addAttribute("ausgewaehlteAufgaben", new ArrayList<Long>());
        
        return "kursbetreuer/aufgabe/aufgabe-generator";
    }

    /**
     * Verarbeitet die Generierung einer neuen Aufgabe.
     *
     * @param kurseinheitId Die ID der Kurseinheit, für die eine Aufgabe generiert werden soll
     * @param thema Das Thema für die neue Aufgabe
     * @param ausgewaehlteAufgaben Liste der IDs von Beispielaufgaben (optional)
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kurseinheit-Detailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ERSTELLEN,
            beschreibung = "Aufgabe zum Thema \"{1}\" für Kurseinheit mit ID {0} generiert"
    )
    @PostMapping("/kurseinheiten/{kurseinheitId}/aufgaben/generator")
    public String generiereAufgabe(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam("thema") String thema,
            @RequestParam(value = "ausgewaehlteAufgaben", required = false) List<Long> ausgewaehlteAufgaben,
            RedirectAttributes redirectAttributes) {
        
        // Prüfen, ob die Kurseinheit existiert
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Kurseinheit nicht gefunden.");
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        // Prüfen, ob ein Thema angegeben wurde
        if (thema == null || thema.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Bitte geben Sie ein Thema für die zu generierende Aufgabe an.");
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/generator";
        }
        
        try {
            // Aufgabe generieren
            AufgabeDto generierteAufgabe = aufgabenGeneratorService.generiereAufgabe(thema, kurseinheitId, ausgewaehlteAufgaben);
            
            // Aufgabe speichern
            AufgabeDto erstellteAufgabe = aufgabenGeneratorService.erstelleGenerierteAufgabe(generierteAufgabe);
            
            // Erfolgsmeldung anzeigen
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Aufgabe zum Thema \"" + thema + "\" wurde erfolgreich generiert.");
            
            // Direkt zur bearbeiteten Aufgabe weiterleiten
            return "redirect:/kursbetreuer/aufgaben/" + erstellteAufgabe.getId() + "/bearbeiten";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler bei der Generierung der Aufgabe: " + e.getMessage());
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/generator";
        }
    }

}