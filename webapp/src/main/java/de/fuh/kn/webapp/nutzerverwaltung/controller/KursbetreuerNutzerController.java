package de.fuh.kn.webapp.nutzerverwaltung.controller;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.belegung.BelegungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.util.SemesterDateUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller für die Verwaltung von Nutzern durch Kursbetreuer.
 * Stellt Funktionen zum Anzeigen, Bearbeiten, Erstellen und Löschen von Studenten und Kursbetreuern bereit.
 */
@Controller
@RequestMapping("/kursbetreuer/nutzer")
@Slf4j
public class KursbetreuerNutzerController {

    private final NutzerService nutzerService;
    private final BelegungService belegungService;
    private final KursService kursService;
    private final AktivitaetsService aktivitaetsService;

    /**
     * Konstruktor für die Dependency Injection.
     *
     * @param nutzerService Service für die Verwaltung von Nutzern
     * @param belegungService Service für die Verwaltung von Belegungen
     * @param kursService Service für die Verwaltung von Kursen
     */
    @Autowired
    public KursbetreuerNutzerController(NutzerService nutzerService,
                                        BelegungService belegungService,
                                        KursService kursService,
                                        AktivitaetsService aktivitaetsService,
                                        AktivitaetMapper aktivitaetMapper) {
        this.nutzerService = nutzerService;
        this.belegungService = belegungService;
        this.kursService = kursService;
        this.aktivitaetsService = aktivitaetsService;
    }

    /**
     * Zeigt eine Übersicht aller Nutzer an.
     *
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping
    public String showNutzerverwaltung(Model model) {
        List<StudentDTO> studenten = nutzerService.getAlleStudenten();
        List<KursbetreuerDTO> kursbetreuer = nutzerService.getAlleKursbetreuer();
        
        model.addAttribute("studenten", studenten);
        model.addAttribute("kursbetreuer", kursbetreuer);
        
        return "kursbetreuer/nutzerverwaltung/nutzerverwaltung";
    }

    /**
     * Zeigt die Details eines Studenten an, inkl. Belegungen und Aktivitäten.
     *
     * @param studentId ID des Studenten
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/student/{studentId}")
    public String showStudentDetails(@PathVariable Long studentId, Model model) {
        try {
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Student nicht gefunden");
                return "error";
            }
            
            StudentDTO student = studentOpt.get();
            
            // Belegungen des Studenten laden
            List<BelegungDTO> belegungen = belegungService.getAllEnrollmentsByStudent(student);
            
            // Aktivitäten des Studenten laden
            Page<AktivitaetDTO> aktivitaeten = aktivitaetsService.findeAktivitaetenFuerNutzerPaged(student,0, 10);

            model.addAttribute("student", student);
            model.addAttribute("belegungen", belegungen);
            model.addAttribute("aktivitaeten", aktivitaeten);
            model.addAttribute("isKursbetreuer", true);
            
            return "kursbetreuer/nutzerverwaltung/student-details";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Studentendaten: " + e.getMessage());
            log.error("Fehler beim Laden der Studentendaten von Student {}", studentId, e);
            return "error";
        }
    }

    /**
     * Zeigt ein Formular zum Bearbeiten der Studentendaten an.
     *
     * @param studentId ID des Studenten
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/student/edit/{studentId}")
    public String showEditStudentForm(@PathVariable Long studentId, Model model) {
        try {
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Student nicht gefunden");
                return "error";
            }
            
            StudentDTO student = studentOpt.get();
            model.addAttribute("student", student);
            
            return "kursbetreuer/nutzerverwaltung/student-edit";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Studentendaten: " + e.getMessage());
            log.error("Fehler beim Laden der Studentendaten von Student {}", studentId, e);
            return "error";
        }
    }

    /**
     * Speichert die geänderten Studentendaten.
     *
     * @param studentId ID des Studenten
     * @param student Die geänderten Studentendaten
     * @param result BindingResult für Validierungsfehler
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.NUTZER_BEARBEITEN,
            beschreibung = "Student {1} bearbeitet",
            mitParametern = true)
    @PostMapping("/student/edit/{studentId}")
    public String saveStudent(
            @PathVariable Long studentId,
            @Valid @ModelAttribute("student") StudentDTO student,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "kursbetreuer/nutzerverwaltung/student-edit";
        }
        
        try {
            StudentDTO updatedStudent = nutzerService.aktualisiereStudent(studentId, student);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Studentendaten erfolgreich aktualisiert");
            
            return "redirect:/kursbetreuer/nutzer/student/" + updatedStudent.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Bearbeiten der Studentendaten von Student {}", studentId, e);
            return "redirect:/kursbetreuer/nutzer/student/edit/" + studentId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Speichern der Studentendaten: " + e.getMessage());
            log.error("Fehler beim Bearbeiten der Studentendaten von Student {}", studentId, e);
            return "redirect:/kursbetreuer/nutzer/student/edit/" + studentId;
        }
    }

    /**
     * Setzt das Passwort eines Nutzers zurück.
     *
     * @param nutzerId ID des Nutzers
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.PASSWORT_AENDERN,
            mitParametern = false)
    @PostMapping("/passwort/reset/{nutzerId}")
    public String resetPassword(
            @PathVariable Long nutzerId,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            String neuesPasswort = nutzerService.setzePasswortZurueck(nutzerId);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Passwort erfolgreich zurückgesetzt. Neues Passwort: " + neuesPasswort);
            
            // Prüfen, ob es sich um einen Studenten oder einen Kursbetreuer handelt
            Optional<StudentDTO> student = nutzerService.getStudentById(nutzerId);
            if (student.isPresent()) {

                //Für Aktivitätsprotokollierung
                model.addAttribute("student", student.get());

                return "redirect:/kursbetreuer/nutzer/student/" + nutzerId;
            } else {
                Optional<KursbetreuerDTO> kursbetreuer = nutzerService.getKursbetreuerById(nutzerId);
                if (kursbetreuer.isPresent()) {
                    //Für Aktivitätsprotokollierung
                    model.addAttribute("kursbetreuer", kursbetreuer.get());

                    return "redirect:/kursbetreuer/nutzer/kursbetreuer/" + nutzerId;
                } else {
                    return "redirect:/kursbetreuer/nutzer";
                }
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Zurücksetzen des Passworts von Nutzer {}", nutzerId, e);
            return "redirect:/kursbetreuer/nutzer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Zurücksetzen des Passworts: " + e.getMessage());
            log.error("Fehler beim Zurücksetzen des Passworts von Nutzer {}", nutzerId, e);
            return "redirect:/kursbetreuer/nutzer";
        }
    }

    /**
     * Zeigt die Bestätigungsseite zum Löschen eines Studenten an.
     *
     * @param studentId ID des zu löschenden Studenten
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Löschbestätigung
     */
    @GetMapping("/student/{studentId}/loeschen")
    public String showDeleteStudentConfirmation(@PathVariable("studentId") Long studentId, Model model) {
        try {
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Student nicht gefunden");
                return "error";
            }
            
            StudentDTO student = studentOpt.get();
            model.addAttribute("student", student);
            
            return "kursbetreuer/nutzerverwaltung/student-loeschen";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Studentendaten: " + e.getMessage());
            log.error("Fehler beim Laden der Studentendaten von Student {}", studentId, e);
            return "error";
        }
    }

    /**
     * Löscht einen Studenten nach Bestätigung.
     *
     * @param studentId ID des Studenten
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.NUTZER_LOESCHEN,
            mitParametern = false)
    @PostMapping("/student/delete/{studentId}")
    public String deleteStudent(
            @PathVariable Long studentId,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Student nicht gefunden");
                return "redirect:/kursbetreuer/nutzer";
            }
            
            StudentDTO student = studentOpt.get();
            String studentName = student.getVorname() != null && student.getNachname() != null ? 
                                 student.getVorname() + " " + student.getNachname() : 
                                 "Matrikelnr. " + student.getMatrikelnummer();
            
            nutzerService.loescheStudent(studentId);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Student " + studentName + " erfolgreich gelöscht");

            //Für Aktivitätsprotokollierung
            model.addAttribute("student", student);
            
            return "redirect:/kursbetreuer/nutzer";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Löschen von Student {}", studentId, e);
            return "redirect:/kursbetreuer/nutzer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Löschen des Studenten: " + e.getMessage());
            log.error("Fehler beim Löschen von Student {}", studentId, e);
            return "redirect:/kursbetreuer/nutzer";
        }
    }

    /**
     * Zeigt ein Formular zum Erstellen eines neuen Kursbetreuers an.
     *
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/kursbetreuer/create")
    public String showCreateKursbetreuerForm(Model model) {
        model.addAttribute("kursbetreuer", new KursbetreuerDTO());
        return "kursbetreuer/nutzerverwaltung/kursbetreuer-create";
    }

    /**
     * Speichert einen neuen Kursbetreuer.
     *
     * @param kursbetreuer Die Daten des neuen Kursbetreuers
     * @param result BindingResult für Validierungsfehler
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.NUTZER_ANLEGEN,
            beschreibung = "Neuer Kursbetreuer angelegt",
            mitParametern = true)
    @PostMapping("/kursbetreuer/create")
    public String saveKursbetreuer(
            @Valid @ModelAttribute("kursbetreuer") KursbetreuerDTO kursbetreuer,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (result.hasErrors()) {
            return "kursbetreuer/nutzerverwaltung/kursbetreuer-create";
        }
        
        try {
            KursbetreuerDTO neuerKursbetreuer = nutzerService.erstelleKursbetreuer(kursbetreuer);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Kursbetreuer erfolgreich erstellt. Passwort: " + neuerKursbetreuer.getKlartext_passwort());

            //Für Aktivitätsprotokollierung
            model.addAttribute("kursbetreuer", neuerKursbetreuer);

            return "redirect:/kursbetreuer/nutzer/kursbetreuer/" + neuerKursbetreuer.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Erstellen eines Kursbetreuers", e);
            return "redirect:/kursbetreuer/nutzer/kursbetreuer/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Erstellen des Kursbetreuers: " + e.getMessage());
            log.error("Fehler beim Erstellen eines Kursbetreuers", e);
            return "redirect:/kursbetreuer/nutzer/kursbetreuer/create";
        }
    }

    /**
     * Zeigt die Details eines Kursbetreuers an.
     *
     * @param kursbetreuerID ID des Kursbetreuers
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/kursbetreuer/{kursbetreuerID}")
    public String showKursbetreuerDetails(@PathVariable Long kursbetreuerID, Model model) {
        try {
            Optional<KursbetreuerDTO> kursbetreuerOpt = nutzerService.getKursbetreuerById(kursbetreuerID);
            if (kursbetreuerOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Kursbetreuer nicht gefunden");
                return "error";
            }
            
            KursbetreuerDTO kursbetreuer = kursbetreuerOpt.get();
            
            // Aktivitäten des Kursbetreuers laden
            Page<AktivitaetDTO> aktivitaeten = aktivitaetsService.findeAktivitaetenFuerNutzerPaged(kursbetreuer, 0, 10);

            model.addAttribute("kursbetreuer", kursbetreuer);
            model.addAttribute("aktivitaeten", aktivitaeten);
            model.addAttribute("isKursbetreuer", true);
            
            return "kursbetreuer/nutzerverwaltung/kursbetreuer-details";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Kursbetreuer-Daten: " + e.getMessage());
            log.error("Fehler beim Laden des Kursbetreuers {}", kursbetreuerID, e);
            return "error";
        }
    }

    /**
     * Zeigt ein Formular zum Bearbeiten eines Kursbetreuers an.
     *
     * @param kursbetreuerID ID des Kursbetreuers
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/kursbetreuer/edit/{kursbetreuerID}")
    public String showEditKursbetreuerForm(@PathVariable Long kursbetreuerID, Model model) {
        try {
            Optional<KursbetreuerDTO> kursbetreuerOpt = nutzerService.getKursbetreuerById(kursbetreuerID);
            if (kursbetreuerOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Kursbetreuer nicht gefunden");
                return "error";
            }
            
            KursbetreuerDTO kursbetreuer = kursbetreuerOpt.get();
            model.addAttribute("kursbetreuer", kursbetreuer);
            
            return "kursbetreuer/nutzerverwaltung/kursbetreuer-edit";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Kursbetreuer-Daten: " + e.getMessage());
            log.error("Fehler beim Laden des Kursbetreuers {}", kursbetreuerID, e);
            return "error";
        }
    }
    
    /**
     * Speichert die geänderten Kursbetreuer-Daten.
     *
     * @param kursbetreuerID ID des Kursbetreuers
     * @param kursbetreuer Die geänderten Kursbetreuer-Daten
     * @param result BindingResult für Validierungsfehler
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.NUTZER_BEARBEITEN,
            mitParametern = true)
    @PostMapping("/kursbetreuer/edit/{kursbetreuerID}")
    public String saveKursbetreuer(
            @PathVariable Long kursbetreuerID,
            @Valid @ModelAttribute("kursbetreuer") KursbetreuerDTO kursbetreuer,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (result.hasErrors()) {
            return "kursbetreuer/nutzerverwaltung/kursbetreuer-edit";
        }
        
        try {
            KursbetreuerDTO updatedKursbetreuer = nutzerService.aktualisiereKursbetreuer(kursbetreuerID, kursbetreuer);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Kursbetreuer-Daten erfolgreich aktualisiert");

            //Für Aktivitätsprotokollierung
            model.addAttribute("kursbetreuer", updatedKursbetreuer);
            
            return "redirect:/kursbetreuer/nutzer/kursbetreuer/" + updatedKursbetreuer.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Bearbeiten des Kursbetreuers {}", kursbetreuerID, e);
            return "redirect:/kursbetreuer/nutzer/kursbetreuer/edit/" + kursbetreuerID;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Speichern der Kursbetreuer-Daten: " + e.getMessage());
            log.error("Fehler beim Bearbeiten des Kursbetreuers {}", kursbetreuerID, e);
            return "redirect:/kursbetreuer/nutzer/kursbetreuer/edit/" + kursbetreuerID;
        }
    }

    /**
     * Zeigt die Bestätigungsseite zum Löschen eines Kursbetreuers an.
     *
     * @param kursbetreuerID ID des zu löschenden Kursbetreuers
     * @param model Das Model für die View
     * @return Der Name der Template-Datei für die Löschbestätigung
     */
    @GetMapping("/kursbetreuer/{kursbetreuerID}/loeschen")
    public String showDeleteKursbetreuerConfirmation(@PathVariable("kursbetreuerID") Long kursbetreuerID, Model model) {
        try {
            Optional<KursbetreuerDTO> kursbetreuerOpt = nutzerService.getKursbetreuerById(kursbetreuerID);
            if (kursbetreuerOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Kursbetreuer nicht gefunden");
                return "error";
            }
            
            KursbetreuerDTO kursbetreuer = kursbetreuerOpt.get();
            model.addAttribute("kursbetreuer", kursbetreuer);
            
            return "kursbetreuer/nutzerverwaltung/kursbetreuer-loeschen";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Kursbetreuer-Daten: " + e.getMessage());
            log.error("Fehler beim Laden des Kursbetreuers {}", kursbetreuerID, e);
            return "error";
        }
    }

    /**
     * Löscht einen Kursbetreuer nach Bestätigung.
     * Verhindert, dass ein Kursbetreuer sich selbst löscht.
     *
     * @param kursbetreuerID ID des Kursbetreuers
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.NUTZER_LOESCHEN,
            mitParametern = false)
    @PostMapping("/kursbetreuer/delete/{kursbetreuerID}")
    public String deleteKursbetreuer(
            @PathVariable Long kursbetreuerID,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        try {
            // Prüfen, ob der aktuelle Benutzer versucht, sich selbst zu löschen
            if (currentUser instanceof KursbetreuerUserDetails kursbetreuerDetails) {
                if (kursbetreuerDetails.getId().equals(kursbetreuerID)) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                            "Sie können sich nicht selbst löschen.");
                    return "redirect:/kursbetreuer/nutzer/kursbetreuer/" + kursbetreuerID;
                }
            }
            
            Optional<KursbetreuerDTO> kursbetreuerOpt = nutzerService.getKursbetreuerById(kursbetreuerID);
            if (kursbetreuerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Kursbetreuer nicht gefunden");
                return "redirect:/kursbetreuer/nutzer";
            }
            
            KursbetreuerDTO kursbetreuer = kursbetreuerOpt.get();
            String kursbetreuerName = kursbetreuer.getVorname() + " " + kursbetreuer.getNachname();
            
            nutzerService.loescheKursbetreuer(kursbetreuerID);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Kursbetreuer " + kursbetreuerName + " erfolgreich gelöscht");

            //Für Aktivitätsprotokollierung
            model.addAttribute("kursbetreuer", kursbetreuer);
            
            return "redirect:/kursbetreuer/nutzer";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Löschen des Kursbetreuers {}", kursbetreuerID, e);
            return "redirect:/kursbetreuer/nutzer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Löschen des Kursbetreuers: " + e.getMessage());
            log.error("Fehler beim Löschen des Kursbetreuers {}", kursbetreuerID, e);
            return "redirect:/kursbetreuer/nutzer";
        }
    }
    
    /**
     * Zeigt ein Formular zum Hinzufügen einer neuen Belegung für einen Studenten an.
     *
     * @param studentId ID des Studenten
     * @param preselectedKursId Vorausgewählte Kurs-ID (optional)
     * @param returnTo URL-Parameter, der angibt, wohin nach dem Speichern zurückgeleitet werden soll (optional)
     * @param model Spring Model
     * @return Template-Name
     */
    @GetMapping("/student/{studentId}/belegung/add")
    public String showAddBelegungForm(
            @PathVariable Long studentId, 
            @RequestParam(required = false) Long preselectedKursId,
            @RequestParam(required = false, defaultValue = "student") String returnTo,
            Model model) {
        try {
            // Student über ID laden
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Student nicht gefunden");
                return "error";
            }
            StudentDTO student = studentOpt.get();
            
            // Alle Belegungen des Studenten laden
            List<BelegungDTO> studentBelegungen = belegungService.getAllEnrollmentsByStudent(student);
            List<Long> enrolledKursIds = studentBelegungen.stream()
                    .map(BelegungDTO::getKursId)
                    .collect(Collectors.toList());
            
            // Alle Kurse laden
            List<KursDTO> alleKurse = kursService.getAlleKurse();
            
            // Verfügbare Kurse filtern (die, die der Student noch nicht belegt)
            List<KursDTO> verfuegbareKurse = alleKurse.stream()
                    .filter(kurs -> !enrolledKursIds.contains(kurs.getId()))
                    .collect(Collectors.toList());
            
            // Bestimme das nächstliegende Startdatum und Enddatum des Semesters
            LocalDate now = LocalDate.now();
            LocalDate[] semesterDates = SemesterDateUtil.calculateSemesterDates(now);
            LocalDate startDatum = semesterDates[0];
            LocalDate endDatum = semesterDates[1];
            
            // Falls eine Kurs-ID als Parameter übergeben wurde, prüfen wir ob dieser verfügbar ist
            KursDTO preselectedKurs = null;
            if (preselectedKursId != null) {
                preselectedKurs = kursService.getKursById(preselectedKursId);
                if (preselectedKurs != null && enrolledKursIds.contains(preselectedKursId)) {
                    model.addAttribute("warningMessage", "Der Student belegt diesen Kurs bereits.");
                    preselectedKurs = null;
                }
            }
            
            model.addAttribute("student", student);
            model.addAttribute("verfuegbareKurse", verfuegbareKurse);
            model.addAttribute("startDatum", startDatum);
            model.addAttribute("endDatum", endDatum);
            model.addAttribute("preselectedKurs", preselectedKurs);
            model.addAttribute("returnTo", returnTo);
            
            return "kursbetreuer/nutzerverwaltung/student-belegung-add";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fehler beim Laden der Daten: " + e.getMessage());
            log.error("Fehler beim Laden der Daten von Student {}", studentId, e);
            return "error";
        }
    }

    /**
     * Verarbeitet das Hinzufügen einer neuen Belegung für einen Studenten.
     *
     * @param studentId ID des Studenten
     * @param kursId ID des Kurses
     * @param startDatum Startdatum der Belegung
     * @param endDatum Enddatum der Belegung
     * @param returnTo URL-Parameter, der angibt, wohin nach dem Speichern zurückgeleitet werden soll
     * @param redirectAttributes RedirectAttributes für Flash-Nachrichten
     * @return Redirect-URL
     */
    @PostMapping("/student/{studentId}/belegung/add")
    public String addBelegungToStudent(
            @PathVariable Long studentId,
            @RequestParam Long kursId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDatum,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDatum,
            @RequestParam(required = false, defaultValue = "student") String returnTo,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Bestehenden Studenten laden
            Optional<StudentDTO> studentOpt = nutzerService.getStudentById(studentId);
            if (studentOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Student nicht gefunden");
                return "redirect:/kursbetreuer/nutzer";
            }
            StudentDTO student = studentOpt.get();
            
            // Kurs laden
            KursDTO kurs = kursService.getKursById(kursId);
            if (kurs == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Kurs nicht gefunden");
                if ("kurs".equals(returnTo) && kursId != null) {
                    return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
                } else {
                    return "redirect:/kursbetreuer/nutzer/student/" + studentId;
                }
            }
            
            // Belegung erstellen
            BelegungDTO belegung = belegungService.addStudentToKurs(student, kurs, startDatum, endDatum);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Belegung für Kurs " + kurs.getName() + " erfolgreich hinzugefügt");
            
            // Je nach returnTo-Parameter zurückleiten
            if ("kurs".equals(returnTo)) {
                return "redirect:/kursbetreuer/belegungen/kurs/" + kursId;
            } else {
                return "redirect:/kursbetreuer/nutzer/student/" + studentId;
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Erstellen einer neuen Belegung für Student {}", studentId, e);
            return String.format("redirect:/kursbetreuer/nutzer/student/%d/belegung/add?returnTo=%s", studentId, returnTo);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fehler beim Erstellen der Belegung: " + e.getMessage());
            log.error("Fehler beim Erstellen einer neuen Belegung für Student {}", studentId, e);
            return String.format("redirect:/kursbetreuer/nutzer/student/%d/belegung/add?returnTo=%s", studentId, returnTo);
        }
    }
}