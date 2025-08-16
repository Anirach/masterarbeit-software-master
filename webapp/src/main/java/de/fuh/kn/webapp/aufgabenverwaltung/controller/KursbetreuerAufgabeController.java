package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
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
 * Controller für die Verwaltung von Aufgaben durch Kursbetreuer.
 * Dieser Controller stellt Funktionen zum Anzeigen, Erstellen, Bearbeiten und Löschen von Aufgaben bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerAufgabeController {

    private final AufgabeService aufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final KurseinheitService kurseinheitService;
    private final KursService kursService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param aufgabeService     Der Service für die Verwaltung von Aufgaben
     * @param teilaufgabeService Der Service für die Verwaltung von Teilaufgaben
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     */
    @Autowired
    public KursbetreuerAufgabeController(
            AufgabeService aufgabeService,
            TeilaufgabeService teilaufgabeService,
            KurseinheitService kurseinheitService, KursService kursService) {
        this.aufgabeService = aufgabeService;
        this.teilaufgabeService = teilaufgabeService;
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;
    }

    /**
     * Erstellt eine neue Aufgabe mit einem Platzhalter-Titel und leitet zur Bearbeitungsansicht weiter.
     *
     * @param kurseinheitId Die ID der Kurseinheit, zu der die Aufgabe hinzugefügt werden soll
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Bearbeitungsseite der neuen Aufgabe
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ERSTELLEN,
            beschreibung = "Neue Aufgabe erstellt"
    )
    @GetMapping("/kurseinheiten/{kurseinheitId}/aufgaben/neu")
    public String createNewAufgabe(
            @PathVariable(value = "kurseinheitId", required = false) Long kurseinheitId,
            RedirectAttributes redirectAttributes) {
        // Prüfen, ob die Kurseinheit existiert
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        // Neue Aufgabe erstellen und mit der Kurseinheit-ID vorausfüllen
        AufgabeDto aufgabeDto = new AufgabeDto();
        aufgabeDto.setKurseinheitId(kurseinheitId);
        aufgabeDto.setEinfach(true); // Standard: einfache Aufgabe
        aufgabeDto.setTitel("Neue Aufgabe"); // Platzhalter-Titel

        // Eine Teilaufgabe mit Platzhalter für EINFACH-Typ erstellen
        TeilaufgabeDto teilaufgabeDto = new TeilaufgabeDto();
        teilaufgabeDto.setReihenfolge(1);
        teilaufgabeDto.setAufgabenstellungMarkdown("Aufgabenstellung hier eingeben..."); // Platzhalter für Markdown
        List<TeilaufgabeDto> teilaufgaben = new ArrayList<>();
        teilaufgaben.add(teilaufgabeDto);
        aufgabeDto.setTeilaufgaben(teilaufgaben);

        // Aufgabe speichern
        AufgabeDto gespeicherteAufgabe = aufgabeService.erstelleAufgabe(aufgabeDto);
        
        // Erfolgsmeldung hinzufügen
        redirectAttributes.addFlashAttribute("successMessage", 
                "Neue Aufgabe wurde erstellt. Sie können sie jetzt bearbeiten.");
                
        // Weiterleitung zur Bearbeitungsseite der neuen Aufgabe
        return "redirect:/kursbetreuer/aufgaben/" + gespeicherteAufgabe.getId() + "/bearbeiten";
    }

    /**
     * Zeigt das Formular zum Bearbeiten einer bestehenden Aufgabe an.
     *
     * @param id    Die ID der zu bearbeitenden Aufgabe
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Aufgaben-Formular
     */
    @GetMapping("/aufgaben/{id}/bearbeiten")
    public String showEditAufgabeForm(@PathVariable("id") Long id, Model model) {
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(id);
        if (aufgabe == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        // Kurseinheit laden
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabe.getKurseinheitId());
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        //Kurs laden
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());

        model.addAttribute("aufgabe", aufgabe);
        model.addAttribute("kurs", kurs);
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("isNew", false);

        return "kursbetreuer/aufgabe/aufgabe-bearbeiten";
    }

    /**
     * Verarbeitet das Speichern einer neuen oder bearbeiteten Aufgabe.
     *
     * @param aufgabe              Das AufgabeDto mit den eingegebenen Daten
     * @param teilaufgabenData     Die Liste der Teilaufgaben-Daten
     * @param redirectAttributes   Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Aufgabenansicht oder Kurseinheit-Detailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_BEARBEITEN,
            beschreibung = "Aufgabe \"{0}\" gespeichert"
    )
    @PostMapping("/aufgaben/speichern")
    public String saveAufgabe(
            @ModelAttribute("aufgabe") AufgabeDto aufgabe,
            RedirectAttributes redirectAttributes) {

        AufgabeDto gespeicherteAufgabe;
        // Service entscheidet, ob neue Aufgabe erstellt oder bestehende aktualisiert wird
        if (aufgabe.getId() == null) {
            gespeicherteAufgabe = aufgabeService.erstelleAufgabe(aufgabe);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Die Aufgabe \"" + gespeicherteAufgabe.getTitel() + "\" wurde erfolgreich erstellt.");
        } else {
            gespeicherteAufgabe = aufgabeService.aktualisiereAufgabe(aufgabe);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Die Aufgabe \"" + gespeicherteAufgabe.getTitel() + "\" wurde erfolgreich aktualisiert.");
        }

        return "redirect:/kursbetreuer/aufgaben/"+gespeicherteAufgabe.getId()+"/bearbeiten";
    }


    /**
     * Zeigt die Bestätigungsseite zum Löschen einer Aufgabe an.
     *
     * @param id    Die ID der zu löschenden Aufgabe
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Löschbestätigung
     */
    @GetMapping("/aufgaben/{id}/loeschen")
    public String showDeleteConfirmation(@PathVariable("id") Long id, Model model) {
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(id);
        if (aufgabe == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        // Kurseinheit laden
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabe.getKurseinheitId());
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());

        model.addAttribute("aufgabe", aufgabe);
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kurs", kurs);

        return "kursbetreuer/aufgabe/aufgabe-loeschen";
    }

    /**
     * Löscht eine Aufgabe anhand ihrer ID nach Bestätigung.
     *
     * @param id                 Die ID der zu löschenden Aufgabe
     * @param confirmDelete      Bestätigung vom Formular
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kurseinheit-Detailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_LOESCHEN,
            beschreibung = "Aufgabe \"{0}\" gelöscht"
    )
    @PostMapping("/aufgaben/{id}/loeschen")
    public String deleteAufgabePost(
            @PathVariable("id") Long id, 
            @RequestParam(value = "confirmDelete", required = false) String confirmDelete,
            RedirectAttributes redirectAttributes, Model model) {
        
        // Aufgabe-Namen und Kurseinheit-ID vor dem Löschen abrufen
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(id);
        if (aufgabe == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }

        String aufgabeTitel = aufgabe.getTitel();
        Long kurseinheitId = aufgabe.getKurseinheitId();
        
        // Prüfen, ob die Löschung bestätigt wurde
        if (confirmDelete == null) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Die Löschung muss bestätigt werden.");
            return "redirect:/kursbetreuer/aufgaben/" + id + "/loeschen";
        }

        // Aufgabe löschen
        aufgabeService.loescheAufgabe(id);
        model.addAttribute("aufgabe", aufgabe);

        // Erfolgsmeldung als Flash-Attribut hinzufügen
        redirectAttributes.addFlashAttribute("successMessage",
                "Die Aufgabe \"" + aufgabeTitel + "\" wurde erfolgreich gelöscht.");

        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId;
    }
}