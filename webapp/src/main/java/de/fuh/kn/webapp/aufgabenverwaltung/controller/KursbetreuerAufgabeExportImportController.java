package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.export.AufgabeExportDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabenImportService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.export.AufgabenExportService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
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
import java.util.List;

/**
 * Controller für den Export und Import von Aufgaben.
 * Dieser Controller stellt Funktionen zum Exportieren und Importieren von Aufgaben bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
public class KursbetreuerAufgabeExportImportController {

    private final KurseinheitService kurseinheitService;
    private final KursService kursService;
    private final AufgabenExportService aufgabenExportService;
    private final AufgabenImportService aufgabenImportService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param aufgabenExportService Der Service für den Export von Aufgaben
     * @param aufgabenImportService Der Service für den Import von Aufgaben
     */
    @Autowired
    public KursbetreuerAufgabeExportImportController(
            KurseinheitService kurseinheitService,
            KursService kursService,
            AufgabenExportService aufgabenExportService,
            AufgabenImportService aufgabenImportService) {
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;
        this.aufgabenExportService = aufgabenExportService;
        this.aufgabenImportService = aufgabenImportService;
    }

    /**
     * Exportiert eine Aufgabe als JSON-Datei.
     *
     * @param id Die ID der zu exportierenden Aufgabe
     * @return Die exportierte Aufgabe als JSON-Datei
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_EXPORTIEREN,
            beschreibung = "Aufgabe mit ID {0} exportiert"
    )
    @GetMapping("/aufgaben/{id}/export")
    public ResponseEntity<byte[]> exportAufgabe(@PathVariable("id") Long id) {
        // Aufgabe exportieren
        AufgabeExportDTO aufgabeExport = aufgabenExportService.exportiereAufgabe(id);
        String json = aufgabenExportService.exportiereAlsJson(aufgabeExport);
        
        // Dateiname generieren
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "aufgabe_" + id + "_" + timestamp + ".json";
        
        // HTTP-Response erstellen
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Exportiert alle Aufgaben einer Kurseinheit als JSON-Datei.
     *
     * @param kurseinheitId Die ID der Kurseinheit, deren Aufgaben exportiert werden sollen
     * @return Die exportierten Aufgaben als JSON-Datei
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_EXPORTIEREN,
            beschreibung = "Alle Aufgaben der Kurseinheit mit ID {0} exportiert"
    )
    @GetMapping("/kurseinheiten/{kurseinheitId}/aufgaben/export")
    public ResponseEntity<byte[]> exportAufgabenVonKurseinheit(@PathVariable("kurseinheitId") Long kurseinheitId) {
        // Aufgaben exportieren
        List<AufgabeExportDTO> aufgabenExport = aufgabenExportService.exportiereAufgabenVonKurseinheit(kurseinheitId);
        String json = aufgabenExportService.exportiereAlsJson(aufgabenExport);
        
        // Dateiname generieren
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "kurseinheit_" + kurseinheitId + "_aufgaben_" + timestamp + ".json";
        
        // HTTP-Response erstellen
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Zeigt das Formular zum Importieren von Aufgaben an.
     *
     * @param kurseinheitId Die ID der Kurseinheit, in die die Aufgaben importiert werden sollen
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Import-Formular
     */
    @GetMapping("/kurseinheiten/{kurseinheitId}/aufgaben/import")
    public String showImportForm(@PathVariable("kurseinheitId") Long kurseinheitId, Model model) {
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        
        model.addAttribute("kurseinheit", kurseinheit);
        model.addAttribute("kurs", kurs);
        
        return "kursbetreuer/aufgabe/aufgaben-import-form";
    }

    /**
     * Verarbeitet den Import von Aufgaben aus verschiedenen Datei-Formaten und Kombinationen.
     *
     * @param kurseinheitId Die ID der Kurseinheit, in die die Aufgaben importiert werden sollen
     * @param fileType Der ausgewählte Import-Typ (json, pdf-single, pdf-pair)
     * @param file Die hochgeladene Datei (bei JSON oder einzelner PDF)
     * @param assignmentFile Die hochgeladene Aufgaben-PDF (bei PDF-Paar)
     * @param solutionFile Die hochgeladene Lösungs-PDF (bei PDF-Paar, optional)
     * @param redirectAttributes Attribute für die Weiterleitung, um Nachrichten anzuzeigen
     * @return Eine Weiterleitung zur Kurseinheit-Detailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_IMPORTIEREN,
            beschreibung = "Aufgaben in Kurseinheit mit ID {0} importiert"
    )
    @PostMapping("/kurseinheiten/{kurseinheitId}/aufgaben/import")
    public String importAufgaben(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentFile", required = false) MultipartFile assignmentFile,
            @RequestParam(value = "solutionFile", required = false) MultipartFile solutionFile,
            RedirectAttributes redirectAttributes) {
        
        // Prüfen, ob die Kurseinheit existiert
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Kurseinheit nicht gefunden.");
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        try {
            List<AufgabeDto> importierteAufgaben;
            
            // Je nach gewähltem Import-Typ die entsprechende Methode aufrufen
            switch (fileType) {
                case "json":
                    // JSON-Import
                    if (file == null || file.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine JSON-Datei aus.");
                        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
                    }
                    
                    if (!aufgabenImportService.isJsonFile(file)) {
                        redirectAttributes.addFlashAttribute("errorMessage", 
                                "Die hochgeladene Datei ist keine JSON-Datei.");
                        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
                    }
                    
                    importierteAufgaben = aufgabenImportService.importiereAufgabenAusJson(file, kurseinheitId);
                    break;
                    
                case "pdf-single":
                    // Einzel-PDF-Import
                    if (file == null || file.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine PDF-Datei aus.");
                        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
                    }
                    
                    if (!aufgabenImportService.isPdfFile(file)) {
                        redirectAttributes.addFlashAttribute("errorMessage", 
                                "Die hochgeladene Datei ist keine PDF-Datei.");
                        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
                    }
                    
                    importierteAufgaben = aufgabenImportService.importAufgabenAusPdf(file, kurseinheitId);
                    break;
                    
                case "pdf-pair":
                    // PDF-Paar-Import (Aufgaben + Lösungen)
                    if (assignmentFile == null || assignmentFile.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine Aufgaben-PDF-Datei aus.");
                        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
                    }
                    
                    importierteAufgaben = aufgabenImportService.importAufgabenAusPdfPair(assignmentFile, solutionFile, kurseinheitId);
                    break;
                    
                default:
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Ungültiger Import-Typ. Bitte wählen Sie einen gültigen Import-Typ aus.");
                    return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/aufgaben/import";
            }
            
            // Erfolgsmeldung anzeigen
            redirectAttributes.addFlashAttribute("successMessage", 
                    importierteAufgaben.size() + " Aufgabe(n) wurden erfolgreich importiert.");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Importieren der Aufgaben: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId;
    }
}