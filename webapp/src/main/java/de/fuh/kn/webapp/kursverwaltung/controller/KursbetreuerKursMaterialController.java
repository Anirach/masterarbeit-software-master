package de.fuh.kn.webapp.kursverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Controller für die Verwaltung von Kursmaterialien durch Kursbetreuer.
 * Dieser Controller stellt Funktionen zum Hochladen und Löschen von Kursmaterialien bereit.
 */
@Controller
@RequestMapping("/kursbetreuer")
@Slf4j
public class KursbetreuerKursMaterialController {

    private final KursMaterialService kursMaterialService;
    private final KursService kursService;
    private final KurseinheitService kurseinheitService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param kursMaterialService Der Service für die Verwaltung von Kursmaterialien
     * @param kursService Der Service für die Verwaltung von Kursen
     * @param kurseinheitService Der Service für die Verwaltung von Kurseinheiten
     */
    @Autowired
    public KursbetreuerKursMaterialController(
            KursMaterialService kursMaterialService,
            KursService kursService,
            KurseinheitService kurseinheitService) {
        this.kursMaterialService = kursMaterialService;
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
    }

    /**
     * Zeigt das Formular zum Hochladen eines neuen Kursmaterials für einen Kurs an.
     *
     * @param kursId Die ID des Kurses
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Upload-Formular
     */
    @GetMapping("/kurse/{kursId}/material/neu")
    public String showKursMaterialUploadForm(@PathVariable("kursId") Long kursId, Model model) {
        // Prüfen, ob der Kurs existiert
        KursDTO kurs = kursService.getKursById(kursId);
        if (kurs == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        model.addAttribute("kursId", kursId);
        model.addAttribute("kursName", kurs.getName());
        model.addAttribute("uploadTarget", "kurs");
        
        return "kursbetreuer/kursmaterial/material-upload";
    }

    /**
     * Zeigt das Formular zum Hochladen eines neuen Kursmaterials für eine Kurseinheit an.
     *
     * @param kurseinheitId Die ID der Kurseinheit
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für das Upload-Formular
     */
    @GetMapping("/kurseinheiten/{kurseinheitId}/material/neu")
    public String showKurseinheitMaterialUploadForm(@PathVariable("kurseinheitId") Long kurseinheitId, Model model) {
        // Prüfen, ob die Kurseinheit existiert
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
        if (kurseinheit == null) {
            return "redirect:/kursbetreuer/kursverwaltung";
        }
        
        // Zugehörigen Kurs laden
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        
        model.addAttribute("kurseinheitId", kurseinheitId);
        model.addAttribute("kurseinheitName", kurseinheit.getName());
        model.addAttribute("kursId", kurs.getId());
        model.addAttribute("kursName", kurs.getName());
        model.addAttribute("uploadTarget", "kurseinheit");
        
        return "kursbetreuer/kursmaterial/material-upload";
    }

    /**
     * Verarbeitet das Hochladen einer Datei für einen Kurs.
     *
     * @param kursId Die ID des Kurses
     * @param file Die hochgeladene Datei
     * @param redirectAttributes Attribute für die Weiterleitung
     * @return Eine Weiterleitung zur Kursdetailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURSMATERIAL_HOCHLADEN,
            beschreibung = "Kursmaterial hochgeladen: {1}",
            mitParametern = true)
    @PostMapping("/kurse/{kursId}/material/upload")
    public String uploadKursMaterial(
            @PathVariable("kursId") Long kursId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine Datei aus.");
            return "redirect:/kursbetreuer/kurse/" + kursId + "/material/neu";
        }
        
        try {
            KursMaterialDTO material = kursMaterialService.kursDateiHochladen(kursId, file);
            redirectAttributes.addFlashAttribute("fragmentSuccessMessage",
                "Die Datei \"" + file.getOriginalFilename() + "\" wurde erfolgreich hochgeladen.");

            //Für Aktivitätsprotokollierung
            KursDTO kurs = kursService.getKursById(kursId);
            model.addAttribute("kurs", kurs);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Fehler beim Hochladen der Datei: " + e.getMessage());
            log.error("Fehler beim Hochladen der Datei {}", file.getOriginalFilename(), e);
            return "redirect:/kursbetreuer/kurse/" + kursId + "/material/neu";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Hochladen der Datei {}", file.getOriginalFilename(), e);
            return "redirect:/kursbetreuer/kurse/" + kursId + "/material/neu";
        }
        
        return "redirect:/kursbetreuer/kurse/" + kursId;
    }

    /**
     * Verarbeitet das Hochladen einer Datei für eine Kurseinheit.
     *
     * @param kurseinheitId Die ID der Kurseinheit
     * @param file Die hochgeladene Datei
     * @param redirectAttributes Attribute für die Weiterleitung
     * @return Eine Weiterleitung zur Kurseinheit-Detailseite
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.KURSMATERIAL_HOCHLADEN,
            beschreibung = "Kursmaterial hochgeladen: {1}",
            mitParametern = true)
    @PostMapping("/kurseinheiten/{kurseinheitId}/material/upload")
    public String uploadKurseinheitMaterial(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte wählen Sie eine Datei aus.");
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/material/neu";
        }
        
        try {
            KursMaterialDTO material = kursMaterialService.kurseinheitDateiHochladen(kurseinheitId, file);
            redirectAttributes.addFlashAttribute("fragmentSuccessMessage",
                "Die Datei \"" + file.getOriginalFilename() + "\" wurde erfolgreich hochgeladen.");

            //Für Aktivitätsprotokollierung
            KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
            model.addAttribute("kurseinheit", kurseinheit);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Fehler beim Hochladen der Datei: " + e.getMessage());
            log.error("Fehler beim Hochladen der Datei {}", file.getOriginalFilename(), e);
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/material/neu";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Hochladen der Datei {}", file.getOriginalFilename(), e);
            return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId + "/material/neu";
        }
        
        return "redirect:/kursbetreuer/kurseinheiten/" + kurseinheitId;
    }
    
    /**
     * Ermöglicht das Herunterladen eines Kursmaterials.
     *
     * @param id Die ID des Kursmaterials
     * @return Die Datei als ResponseEntity zum Download
     */
    @GetMapping("/material/{id}/download")
    public ResponseEntity<byte[]> downloadMaterial(@PathVariable("id") Long id) {
        KursMaterialDTO material = kursMaterialService.getKursMaterialById(id);
        
        if (material == null || material.getInhalt() == null) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(material.getMimeType()));
        headers.setContentDispositionFormData("attachment", material.getName());
        
        // Cache-Control-Header setzen:
        // - "must-revalidate": Browser muss die Gültigkeit des Cache vor der Verwendung prüfen
        // - "post-check=0, pre-check=0": Legacy IE-spezifische Einstellungen, die verhindern, 
        //   dass der Browser die Datei zwischenspeichert. Diese bewirken, dass der Browser 
        //   nach dem Laden (post-check) und vor dem Anzeigen (pre-check) sofort die Gültigkeit prüft
        // Zusammen stellen diese Einstellungen sicher, dass der Browser die Datei bei jedem Aufruf 
        // neu vom Server lädt und nicht eine veraltete Version aus dem Cache anzeigt, was besonders 
        // wichtig ist bei Dokumenten, die sich häufig ändern können.
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(material.getInhalt(), headers, HttpStatus.OK);
    }
}
