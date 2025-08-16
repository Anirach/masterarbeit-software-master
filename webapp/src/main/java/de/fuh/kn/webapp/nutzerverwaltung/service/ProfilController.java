package de.fuh.kn.webapp.nutzerverwaltung.service;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsProtokollierungService;
import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.nutzerverwaltung.dto.*;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller für die Verwaltung des Nutzerprofils.
 * Ermöglicht das Anzeigen und Aktualisieren von Profildaten sowie das Ändern des Passworts.
 */
@Slf4j
@Controller
@RequestMapping("/profil")
public class ProfilController {

    private final NutzerService nutzerService;
    private final ProfilMapper profilMapper;
    private final UserDetailsService userDetailsService;
    private final AktivitaetsService aktivitaetsService;
    private final NutzerMapper nutzerMapper;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Konstruktor mit Dependency Injection des NutzerService, ProfilMapper, UserDetailsService,
     * AktivitaetsService und AktivitaetMapper.
     *
     * @param nutzerService Der Service für die Verwaltung von Nutzern.
     * @param profilMapper Der Mapper für die Konvertierung zwischen Nutzer-Entity und ProfilAenderungDTO.
     * @param userDetailsService Der Service für das Laden von UserDetails.
     * @param aktivitaetsService Der Service für die Verwaltung und Abfrage von Aktivitäten.
     * @param aktivitaetMapper Der Mapper für die Konvertierung zwischen Aktivitaet-Entity und AktivitaetDTO.
     */
    public ProfilController(NutzerService nutzerService,
                            ProfilMapper profilMapper,
                            UserDetailsService userDetailsService,
                            AktivitaetsService aktivitaetsService,
                            AktivitaetMapper aktivitaetMapper, NutzerMapper nutzerMapper, AktivitaetsProtokollierungService aktivitaetsProtokollierungService) {
        this.nutzerService = nutzerService;
        this.profilMapper = profilMapper;
        this.userDetailsService = userDetailsService;
        this.aktivitaetsService = aktivitaetsService;
        this.nutzerMapper = nutzerMapper;
        this.aktivitaetsProtokollierungService = aktivitaetsProtokollierungService;
    }

    /**
     * Zeigt die Profilseite des authentifizierten Nutzers an.
     *
     * @param model Das Model für die View.
     * @return Der Name der Template-Datei für die Profilseite.
     */
    @GetMapping
    public String showProfilForm(Model model) {
        // Aktuelle Nutzerdaten abrufen
        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        if (nutzer == null) {
            return "redirect:/login";
        }

        // Profildaten für das Formular vorbereiten
        ProfilAenderungDTO profilAenderungDTO = profilMapper.toDto(nutzer);
        model.addAttribute("profilAenderung", profilAenderungDTO);
        model.addAttribute("passwortAenderung", new PasswortAenderungDTO());
        
        // Aktivitäten des Nutzers laden
        Page<AktivitaetDTO> aktivitaeten = aktivitaetsService.findeAktivitaetenFuerNutzerPaged(nutzer, 0, 10);
        model.addAttribute("aktivitaeten", aktivitaeten);
        model.addAttribute("nutzerId", nutzer.getId());
        model.addAttribute("isKursbetreuer", (nutzer instanceof KursbetreuerDTO));

        return "profil/profil";
    }

    /**
     * Verarbeitet die Aktualisierung der Profildaten.
     *
     * @param profilAenderungDTO Die neuen Profildaten des Nutzers.
     * @param result Das BindingResult-Objekt für Validierungsfehler.
     * @param redirectAttributes Die RedirectAttributes für Flash-Nachrichten.
     * @return Weiterleitung zur Profilseite mit entsprechender Erfolgsmeldung oder Fehlermeldung.
     */
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.PROFIL_BEARBEITEN,
            beschreibung = "Nutzer hat sein Profil aktualisiert",
            mitParametern = true)
    @PostMapping("/update")
    public String updateProfil(
            @Valid @ModelAttribute("profilAenderung") ProfilAenderungDTO profilAenderungDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "profil/profil";
        }

        NutzerDTO nutzerDTO = nutzerService.getAuthenticatedNutzer();
        if (nutzerDTO == null) {
            return "redirect:/login";
        }

        try {
            // Aktualisiere die Nutzerdaten in der Datenbank
            nutzerService.aktualisiereNutzerProfil(nutzerDTO, profilAenderungDTO);
            
            // Aktualisiere die Authentication im SecurityContext
            aktualisierenAuthentication(nutzerDTO.getEmail());
            
            redirectAttributes.addFlashAttribute("successMessage", "Profil erfolgreich aktualisiert");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            log.error("Fehler beim Aktualisieren des Nutzers {}", nutzerDTO.getId(), e);
        }

        return "redirect:/profil";
    }
    
    /**
     * Aktualisiert das Authentication-Objekt im SecurityContext.
     * Dadurch werden die neuen Nutzerdaten in der Session gespeichert.
     *
     * @param username Die E-Mail-Adresse des Nutzers.
     */
    private void aktualisierenAuthentication(String username) {
        // Lade die aktualisierten UserDetails
        UserDetails updatedUserDetails = userDetailsService.loadUserByUsername(username);
        
        // Hole die aktuelle Authentication
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        
        // Erstelle eine neue Authentication mit den aktualisierten UserDetails
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                updatedUserDetails, 
                currentAuth.getCredentials(),
                updatedUserDetails.getAuthorities());
        
        // Setze die neue Authentication im SecurityContext
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    /**
     * Verarbeitet die Änderung des Passworts.
     *
     * @param passwortAenderungDTO Die Daten für die Passwortänderung.
     * @param result Das BindingResult-Objekt für Validierungsfehler.
     * @param redirectAttributes Die RedirectAttributes für Flash-Nachrichten.
     * @return Weiterleitung zur Profilseite mit entsprechender Erfolgsmeldung oder Fehlermeldung.
     */
    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("passwortAenderung") PasswortAenderungDTO passwortAenderungDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        // Validierung der Passwortübereinstimmung
        if (!passwortAenderungDTO.getNeuesPasswort().equals(passwortAenderungDTO.getPasswortBestaetigung())) {
            result.rejectValue("passwortBestaetigung", "error.passwort", "Die Passwörter stimmen nicht überein");
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "Passwörter sind nicht identisch");
            return "redirect:/profil";
        }
        
        // Validierung der Passwortlänge
        if (passwortAenderungDTO.getNeuesPasswort() == null || passwortAenderungDTO.getNeuesPasswort().isEmpty()) {
            result.rejectValue("neuesPasswort", "error.passwort", "Das Passwort darf nicht leer sein");
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "Das Passwort darf nicht leer sein");
            return "redirect:/profil";
        }
        
        if (passwortAenderungDTO.getNeuesPasswort().length() < 8) {
            result.rejectValue("neuesPasswort", "error.passwort", "Das Passwort muss mindestens 8 Zeichen lang sein");
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "Das Passwort muss mindestens 8 Zeichen lang sein");
            return "redirect:/profil";
        }

        if (result.hasErrors()) {
            return "profil/profil";
        }

        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        if (nutzer == null) {
            return "redirect:/login";
        }

        try {
            nutzerService.aenderePasswort(nutzer, passwortAenderungDTO);
            redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Passwort erfolgreich geändert");

            aktivitaetsProtokollierungService.protokolliereAktivitaet(nutzer,
                    AktivitaetsTyp.PASSWORT_AENDERN,
                    "Nutzer hat sein Passwort geändert",
                    null,
                    true,
                    null,
                    null);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", e.getMessage());
            log.error("Fehler beim Aktualisieren des Passworts", e);
        }

        return "redirect:/profil";
    }

}
