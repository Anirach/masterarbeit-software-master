package de.fuh.kn.webapp.nutzerverwaltung.auth;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.nutzerverwaltung.service.RegistrierungDTO;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * Controller für die Verwaltung von Nutzer-Authentifizierung, 
 * Login und Registrierung.
 */
@Controller
@Slf4j
public class LoginRegistrationController {

    private final NutzerService nutzerService;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Konstruktor mit Dependency Injection des NutzerService und RegistrierungMapper.
     *
     * @param nutzerService Der Service für die Verwaltung von Nutzern.
     */
    public LoginRegistrationController(NutzerService nutzerService, AktivitaetsProtokollierungService aktivitaetsProtokollierungService) {
        this.nutzerService = nutzerService;
        this.aktivitaetsProtokollierungService = aktivitaetsProtokollierungService;
    }

    /**
     * Zeigt die Login-Seite an.
     *
     * @return Der Name der Template-Datei für die Login-Seite.
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /**
     * Zeigt die Registrierungsseite für Studierende an.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Registrierungsseite.
     */
    @GetMapping("/registration")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrierung", new RegistrierungDTO());
        return "registration";
    }

    /**
     * Verarbeitet die Registrierung eines Studierenden.
     *
     * @param registrierungDTO Das RegistrierungDTO mit den eingegebenen Registrierungsdaten.
     * @param result Das BindingResult-Objekt für Validierungsfehler.
     * @param model Das Model für die View.
     * @return Eine Weiterleitung zur Login-Seite bei erfolgreicher Registrierung oder zurück zum Registrierungsformular bei Fehlern.
     */
    @PostMapping("/registration")
    public String registerStudent(
            @Valid @ModelAttribute("registrierung") RegistrierungDTO registrierungDTO,
            BindingResult result,
            Model model) {

        // Validierung der Passwortübereinstimmung
        if (!registrierungDTO.getPasswort().equals(registrierungDTO.getPasswortBestaetigung())) {
            result.rejectValue("passwortBestaetigung", "error.passwort", "Die Passwörter stimmen nicht überein");
            model.addAttribute("registrationError", "Passwörter sind nicht identisch");
        }

        // Validierungsfehler anzeigen
        if (result.hasErrors()) {
            return "registration";
        }

        // Prüfen, ob ein Dummy-Student mit dieser Matrikelnummer existiert
        if (!nutzerService.existiertDummyStudentMitMatrikelnummer(registrierungDTO.getMatrikelnummer())) {
            model.addAttribute("registrationError", "Es existiert kein Student mit dieser Matrikelnummer im System. Bitte wenden Sie sich an Ihren Kursbetreuer oder versuchen Sie es in einigen Tagen erneut.");
            return "registration";
        }

        // Prüfen, ob bereits ein registrierter Student mit dieser Matrikelnummer existiert
        if (nutzerService.existiertRegistrierterStudentMitMatrikelnummer(registrierungDTO.getMatrikelnummer())) {
            model.addAttribute("registrationError", "Ein Student mit dieser Matrikelnummer ist bereits registriert.");
            return "registration";
        }

        try {
            StudentDTO registrieredStudent = nutzerService.registriereStudent(registrierungDTO);

            aktivitaetsProtokollierungService.protokolliereAktivitaet(registrieredStudent,
                    AktivitaetsTyp.REGISTRIEREN,
                    "Nutzer hat sich erfolgreich registriert",
                    Map.of("email", registrierungDTO.getEmail(), "matrikelnummer", registrierungDTO.getMatrikelnummer()),
                    true,
                    null,
                    null);

            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("registrationError", e.getMessage());
            log.error("Fehler beim Registrieren eines Studenten mit Matrikelnummer {}", registrierungDTO.getMatrikelnummer(), e);
            return "registration";
        }
    }
}
