package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.export.KursExportDTO;
import de.fuh.kn.webapp.kursverwaltung.export.KursExportService;
import de.fuh.kn.webapp.kursverwaltung.imports.KursImportService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller für den Export und Import von Kursen.
 * Dieser Controller stellt Funktionen zum Exportieren und Importieren von Kursen bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerKursExportImportController {

    private final KursService kursService;
    private final KursExportService kursExportService;
    private final KursImportService kursImportService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param kursExportService Der Service für den Export von Kursen
     * @param kursImportService Der Service für den Import von Kursen
     */
    @Autowired
    public KursbetreuerKursExportImportController(
            KursService kursService,
            KursExportService kursExportService,
            KursImportService kursImportService) {
        this.kursService = kursService;
        this.kursExportService = kursExportService;
        this.kursImportService = kursImportService;
    }

    /**
     * Zeigt die Optionen zum Exportieren eines Kurses an.
     *
     * @param id Die ID des zu exportierenden Kurses
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Export-Optionen
     */
    @GetMapping("/kurse/{id}/export")
    public String showExportOptions(@PathVariable("id") Long id, Model model) {
        KursDTO kurs = kursService.getKursById(id);
        if (kurs == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        model.addAttribute("kurs", kurs);
        return "kursbetreuer/kurs/kurs-export-options";
    }

    /**
     * Exportiert einen Kurs als JSON-Datei.
     *
     * @param id Die ID des zu exportierenden Kurses
     * @return Die exportierte Kurs-Datei als JSON
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURS_EXPORTIEREN,
            beschreibung = "Kurs mit ID {0} als JSON exportiert"
    )
    @GetMapping("/kurse/{id}/export/json")
    public ResponseEntity<byte[]> exportKursAsJson(@PathVariable("id") Long id) {
        // Kurs exportieren
        KursExportDTO kursExport = kursExportService.exportiereKurs(id);
        String json = kursExportService.exportiereAlsJson(kursExport);
        
        // Dateiname generieren
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "kurs_" + id + "_" + timestamp + ".json";
        
        // HTTP-Response erstellen
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Exportiert einen Kurs als ZIP-Datei.
     *
     * @param id Die ID des zu exportierenden Kurses
     * @return Die exportierte Kurs-Datei als ZIP
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURS_EXPORTIEREN,
            beschreibung = "Kurs mit ID {0} als ZIP exportiert"
    )
    @GetMapping("/kurse/{id}/export/zip")
    public ResponseEntity<byte[]> exportKursAsZip(@PathVariable("id") Long id) {
        // Kurs exportieren
        KursExportDTO kursExport = kursExportService.exportiereKurs(id);
        byte[] zipData = kursExportService.exportiereAlsZip(kursExport);
        
        // Dateiname generieren
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "kurs_" + id + "_" + timestamp + ".zip";
        
        // HTTP-Response erstellen
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(zipData);
    }

    /**
     * Zeigt das Formular zum Importieren eines Kurses an.
     *
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Import-Formular
     */
    @GetMapping("/kurse/import")
    public String showImportForm(Model model) {
        return "kursbetreuer/kurs/kurs-import-form";
    }

    /**
     * Verarbeitet den Import eines Kurses aus einer Datei.
     *
     * @param file Die hochgeladene Datei mit dem zu importierenden Kurs
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kursübersicht
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURS_IMPORTIEREN,
            beschreibung = "Kurs importiert"
    )
    @PostMapping("/kurse/import")
    public String importKurs(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        
        // Prüfen, ob eine Datei hochgeladen wurde
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine Datei aus.");
            return "redirect:/kursbetreuer/kurse/import";
        }
        
        try {
            KursDTO importierterKurs;
            
            // Dateiformat prüfen und entsprechend importieren
            if (kursImportService.isJsonFile(file)) {
                importierterKurs = kursImportService.importiereAusJson(file);
            } else if (kursImportService.isZipFile(file)) {
                importierterKurs = kursImportService.importiereAusZip(file);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Ungültiges Dateiformat. Bitte laden Sie eine JSON- oder ZIP-Datei hoch.");
                return "redirect:/kursbetreuer/kurse/import";
            }
            
            // Erfolgsmeldung anzeigen
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Der Kurs \"" + importierterKurs.getName() + "\" wurde erfolgreich importiert.");
            
            return "redirect:/kursbetreuer/kurse/" + importierterKurs.getId();
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Importieren des Kurses: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/kursbetreuer/kurse/import";
    }
}