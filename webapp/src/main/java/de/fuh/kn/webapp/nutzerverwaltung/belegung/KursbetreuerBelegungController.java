package de.fuh.kn.webapp.nutzerverwaltung.belegung;

import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.BelegungDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.util.SemesterDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller für die Verwaltung von Kursbelegungen durch Kursbetreuer.
 * Stellt Funktionen zum Hinzufügen, Entfernen und Verwalten von Belegungen bereit.
 */
@Controller
@RequestMapping("/kursbetreuer/belegungen")
@Slf4j
public class KursbetreuerBelegungController {

    private final BelegungService belegungService;
    private final KursService kursService;
    private final NutzerService nutzerService;

    /**
     * Konstruktor für die Dependency Injection.
     *
     * @param belegungService Service für die Verwaltung von Belegungen
     * @param kursService Service für die Verwaltung von Kursen
     */
    @Autowired
    public KursbetreuerBelegungController(BelegungService belegungService,
                                          KursService kursService,
                                          NutzerService nutzerService) {
        this.belegungService = belegungService;
        this.kursService = kursService;
        this.nutzerService = nutzerService;
    }

    /**
     * Zeigt die Belegungen für einen bestimmten Kurs an.
     *
     * @param kursId ID des Kurses
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/kurs/{kursId}")
    public String showBelegungByKurs(@PathVariable Long kursId, Model model) {
        // Kurs über KursService laden
        KursDTO kursDTO = kursService.getKursById(kursId);
        if (kursDTO == null) {
            throw new NoSuchElementException("Kurs nicht gefunden");
        }
        
        // Belegungen über BelegungService laden
        List<BelegungDTO> belegungen = belegungService.getEnrollmentsByKursWithProgress(kursDTO);
        
        // Alle Studenten über NutzerService laden
        List<StudentDTO> allStudents = nutzerService.getAlleStudenten();
        
        model.addAttribute("kurs", kursDTO);
        model.addAttribute("belegungen", belegungen);
        model.addAttribute("allStudents", allStudents);
        model.addAttribute("heute", LocalDate.now());
        
        return "kursbetreuer/kursbelegung/belegungen-kurs";
    }

    /**
     * Zeigt ein Formular zum Hinzufügen einer neuen Belegung an.
     *
     * @param kursId ID des Kurses
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/add/{kursId}")
    public String showAddBelegungForm(@PathVariable Long kursId, Model model) {
        // Kurs über KursService laden
        KursDTO kursDTO = kursService.getKursById(kursId);
        if (kursDTO == null) {
            throw new NoSuchElementException("Kurs nicht gefunden");
        }
        
        // Alle Belegungen für den Kurs laden
        List<BelegungDTO> existingBelegungen = belegungService.getEnrollmentsByKurs(kursDTO);
        List<Long> enrolledStudentIds = existingBelegungen.stream()
                .map(BelegungDTO::getStudentId)
                .toList();
        
        // Verfügbare Studenten über NutzerService laden
        List<StudentDTO> availableStudents = nutzerService.getAlleStudenten().stream()
                .filter(studentDTO -> !enrolledStudentIds.contains(studentDTO.getId()))
                .collect(Collectors.toList());
        
        // Bestimme das nächstliegende Startdatum (1.4. oder 1.10.)
        LocalDate now = getCurrentDate();
        LocalDate[] semesterDates = SemesterDateUtil.calculateSemesterDates(now);
        LocalDate startDatum = semesterDates[0];
        LocalDate endDatum = semesterDates[1];

        model.addAttribute("kurs", kursDTO);
        model.addAttribute("availableStudents", availableStudents);
        model.addAttribute("startDatum", startDatum);
        model.addAttribute("endDatum", endDatum);
        
        return "kursbetreuer/kursbelegung/belegung-add";
    }

    /**
     * Verarbeitet das Hinzufügen einer neuen Belegung oder mehrerer Belegungen.
     *
     * @param kursId ID des Kurses
     * @param studentId ID des Studenten
     * @param matrikelnummer Matrikelnummer des Studenten
     * @param matrikelnummernListe Liste von Matrikelnummern für Masseneingabe
     * @param startDatum Startdatum der Belegung
     * @param endDatum Enddatum der Belegung
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @PostMapping("/add/{kursId}")
    public String addBelegung(
            @PathVariable Long kursId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String matrikelnummer,
            @RequestParam(required = false) String matrikelnummernListe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDatum,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDatum,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Kurs über KursService laden
            KursDTO kursDTO = kursService.getKursById(kursId);
            if (kursDTO == null) {
                throw new NoSuchElementException("Kurs nicht gefunden");
            }
            
            // Je nach Eingabemethode unterschiedlichen Weg wählen
            if (studentId != null && studentId > 0) {
                StudentDTO studentDTO = nutzerService.getStudentById(studentId).orElseThrow(()->new NoSuchElementException("Student nicht gefunden"));
                
                // Verwende DTO-basierte Methode im Service
                BelegungDTO belegung = belegungService.addStudentToKurs(studentDTO, kursDTO, startDatum, endDatum);
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Belegung für Student " + belegung.getStudentName() + " erfolgreich hinzugefügt.");
            } else if (matrikelnummer != null && !matrikelnummer.isBlank()) {
                // Belegung über Matrikelnummer hinzufügen
                BelegungDTO belegung = belegungService.addStudentByMatrikelnummerToKurs(matrikelnummer, kursId, startDatum, endDatum);
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Belegung für Student " + belegung.getStudentName() + " erfolgreich hinzugefügt.");
            } else if (matrikelnummernListe != null && !matrikelnummernListe.isBlank()) {
                // Mehrere Belegungen über Liste von Matrikelnummern hinzufügen
                
                // Matrikelnummern aus der Liste extrahieren
                List<String> matrikelnummern = new ArrayList<>();
                
                // Sowohl für Komma-getrennte als auch für Zeilenumbruch-getrennte Listen
                for (String line : matrikelnummernListe.split("\\r?\\n")) {
                    for (String m : line.split(",")) {
                        String trimmed = m.trim();
                        if (!trimmed.isEmpty()) {
                            matrikelnummern.add(trimmed);
                        }
                    }
                }
                
                if (matrikelnummern.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Keine gültigen Matrikelnummern gefunden.");
                    return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
                }
                
                // Mehrere Belegungen über den Service erstellen
                List<BelegungDTO> erstellteBelegungen = belegungService.addMultipleStudentsByMatrikelnummerToKurs(
                        matrikelnummern, kursId, startDatum, endDatum);
                
                if (erstellteBelegungen.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Keine Belegungen konnten erstellt werden. Möglicherweise belegen alle Studenten den Kurs bereits.");
                } else {
                    redirectAttributes.addFlashAttribute("successMessage", 
                            erstellteBelegungen.size() + " Belegung(en) erfolgreich hinzugefügt.");
                }
            } else {
                // Weder StudentId noch Matrikelnummer noch Matrikelnummernliste angegeben
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Bitte wählen Sie einen Studenten aus, geben Sie eine Matrikelnummer ein, oder fügen Sie mehrere Matrikelnummern hinzu.");
            }
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Student oder Kurs nicht gefunden.");
            log.error("Fehler beim Erstellen einer Belegung", e);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Erstellen einer Belegung", e);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ein Fehler ist aufgetreten: " + e.getMessage());
            log.error("Fehler beim Erstellen einer Belegung", e);
        }
        
        return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
    }

    /**
     * Zeigt ein Formular zum Bearbeiten einer bestehenden Belegung an.
     *
     * @param belegungId ID der Belegung
     * @param returnTo URL-Parameter, der angibt, wohin nach dem Speichern zurückgeleitet werden soll (kurs oder student)
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/edit/{belegungId}")
    public String showEditBelegungForm(
            @PathVariable Long belegungId, 
            @RequestParam(required = false, defaultValue = "kurs") String returnTo,
            Model model) {
        try {
            // Belegung über den BelegungService laden
            BelegungDTO belegungDTO = belegungService.getBelegungById(belegungId);
            
            // Kurs über KursService laden, um den vollständigen Namen zu erhalten
            KursDTO kursDTO = kursService.getKursById(belegungDTO.getKursId());
            
            model.addAttribute("belegung", belegungDTO);
            model.addAttribute("kursId", belegungDTO.getKursId());
            model.addAttribute("kursName", kursDTO != null ? kursDTO.getName() : "Kurs");
            model.addAttribute("returnTo", returnTo);
            model.addAttribute("studentId", belegungDTO.getStudentId());
            
            return "kursbetreuer/kursbelegung/belegung-edit";
        } catch (NoSuchElementException e) {
            model.addAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Bearbeiten einer Belegung", e);
            return "error";
        }
    }

    /**
     * Verarbeitet das Aktualisieren einer bestehenden Belegung.
     *
     * @param belegungId ID der Belegung
     * @param startDatum Neues Startdatum
     * @param endDatum Neues Enddatum
     * @param returnTo URL-Parameter, der angibt, wohin nach dem Speichern zurückgeleitet werden soll (kurs oder student)
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @PostMapping("/edit/{belegungId}")
    public String updateBelegung(
            @PathVariable Long belegungId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDatum,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDatum,
            @RequestParam(required = false, defaultValue = "kurs") String returnTo,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Belegung über den Service laden
            BelegungDTO belegungDTO = belegungService.getBelegungById(belegungId);
            Long kursId = belegungDTO.getKursId();
            Long studentId = belegungDTO.getStudentId();
            
            BelegungDTO updatedBelegung = belegungService.updateBelegungDates(belegungDTO, startDatum, endDatum);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Belegungsdaten für Student " + updatedBelegung.getStudentName() + " erfolgreich aktualisiert.");
            
            // Je nach returnTo-Parameter zurückleiten
            if ("student".equals(returnTo)) {
                return "redirect:/kursbetreuer/nutzer/student/" + studentId;
            } else {
                return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
            }
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Bearbeiten einer Belegung", e);
            return "redirect:/kursbetreuer/kursverwaltung";
        }
    }

    /**
     * Verarbeitet das Löschen einer Belegung.
     *
     * @param belegungId ID der Belegung
     * @param returnTo URL-Parameter, der angibt, wohin nach dem Löschen zurückgeleitet werden soll (kurs oder student)
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @PostMapping("/delete/{belegungId}")
    public String deleteBelegung(
            @PathVariable Long belegungId,
            @RequestParam(required = false, defaultValue = "kurs") String returnTo,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Belegung über den Service laden
            BelegungDTO belegungDTO = belegungService.getBelegungById(belegungId);
            
            String studentName = belegungDTO.getStudentName();
            Long kursId = belegungDTO.getKursId();
            Long studentId = belegungDTO.getStudentId();
            
            belegungService.removeBelegung(belegungDTO);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Belegung für Student " + studentName + " erfolgreich gelöscht.");
            
            // Je nach returnTo-Parameter zurückleiten
            if ("student".equals(returnTo)) {
                return "redirect:/kursbetreuer/nutzer/student/" + studentId;
            } else {
                return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
            }
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Löschen einer Belegung", e);
            return "redirect:/kursbetreuer/kursverwaltung";
        }
    }
    
    /**
     * Liefert das aktuelle Datum.
     * Diese Methode wurde für bessere Testbarkeit eingeführt, damit sie in Tests überschrieben werden kann.
     * 
     * @return Das aktuelle Datum
     */
    protected LocalDate getCurrentDate() {
        return LocalDate.now();
    }
    
    /**
     * Verarbeitet das Löschen mehrerer Belegungen gleichzeitig.
     *
     * @param belegungIds IDs der zu löschenden Belegungen
     * @param kursId ID des Kurses
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @PostMapping("/massDelete")
    public String massDeleteBelegungen(
            @RequestParam(required = false) List<Long> belegungIds,
            @RequestParam Long kursId,
            RedirectAttributes redirectAttributes) {
        
        if (belegungIds == null || belegungIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Keine Belegungen zum Löschen ausgewählt.");
            return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
        }
        
        try {
            // Konvertiere Long IDs zu BelegungDTOs
            List<BelegungDTO> belegungDTOs = belegungIds.stream()
                    .map(id -> {
                        try {
                            return belegungService.getBelegungById(id);
                        } catch (NoSuchElementException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Verwende DTO-basierte Methode
            List<Long> erfolgreichGeloescht = belegungService.removeBelegungen(belegungDTOs);
            
            if (!erfolgreichGeloescht.isEmpty()) {
                redirectAttributes.addFlashAttribute("successMessage", 
                        erfolgreichGeloescht.size() + " Belegung(en) erfolgreich gelöscht." + 
                        (erfolgreichGeloescht.size() < belegungIds.size() ? 
                        " " + (belegungIds.size() - erfolgreichGeloescht.size()) + " Belegung(en) konnten nicht gelöscht werden." : ""));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Keine Belegungen konnten gelöscht werden. Bitte versuchen Sie es erneut.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Löschen der Belegungen: " + e.getMessage());
            log.error("Fehler beim Löschen von Belegungen", e);
        }
        
        return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
    }
}