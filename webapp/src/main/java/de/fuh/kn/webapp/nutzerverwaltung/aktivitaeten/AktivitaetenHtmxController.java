package de.fuh.kn.webapp.nutzerverwaltung.aktivitaeten;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.KursbetreuerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Controller für HTMX-Anfragen im Zusammenhang mit Aktivitäten.
 * Verarbeitet Filter- und Paginierungsanfragen für Aktivitäten mit einem DTO-Ansatz.
 */
@Slf4j
@Controller
@RequestMapping("/aktivitaeten/htmx")
public class AktivitaetenHtmxController {
    
    private final AktivitaetsService aktivitaetsService;
    private final NutzerService nutzerService;
    
    @Autowired
    public AktivitaetenHtmxController(AktivitaetsService aktivitaetsService, NutzerService nutzerService) {
        this.aktivitaetsService = aktivitaetsService;
        this.nutzerService = nutzerService;
    }
    
    /**
     * Endpunkt zum Filtern und Paginieren von Aktivitäten mit HTMX.
     * Nutzt ein AktivitaetFilterDTO zur Parameterbündelung.
     * Gibt ein HTML-Fragment zurück, das nur die gefilterten Aktivitäten enthält.
     *
     * @param filter DTO mit allen Filter- und Paginierungsparametern
     * @param model Spring MVC Model
     * @return Der View-Name für das Fragment
     */
    @PostMapping("/filter")
    public String filterAktivitaeten(@ModelAttribute AktivitaetFilterDTO filter, Model model) {
        //Studenten dürfen nur eigene Aktivitäten erfragen
        NutzerDTO nutzer = nutzerService.getAuthenticatedNutzer();
        boolean isKursbetreuer = (nutzer instanceof KursbetreuerDTO);
        
        if(nutzer instanceof StudentDTO){
            if(filter.isShowAllUsers() || !Objects.equals(filter.getNutzerId(), nutzer.getId())){
                log.warn("Student {} versucht Aktivitätsdaten von anderen Nutzern abzurufen", nutzer.getId());
                throw new AccessDeniedException("Kein Zugriff auf diese Daten");
            }
        }

        // Datumsbereich prüfen und als Filter anwenden
        LocalDateTime startDate = filter.getStartDateTime();
        LocalDateTime endDate = filter.getEndDateTime();
        
        // Spezifikation für Datumsbereich
        Specification<Aktivitaet> dateRangeSpec = null;
        if (startDate != null && endDate != null) {
            // Nur verwenden, wenn beide Daten gesetzt sind
            dateRangeSpec = (root, query, cb) -> 
                cb.between(root.get("zeitpunkt"), startDate, endDate);
        } else if (startDate != null) {
            // Nur Start-Datum ist gesetzt
            dateRangeSpec = (root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("zeitpunkt"), startDate);
        } else if (endDate != null) {
            // Nur End-Datum ist gesetzt
            dateRangeSpec = (root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("zeitpunkt"), endDate);
        }

        // Gefilterte Aktivitäten holen
        Page<AktivitaetDTO> aktivitaeten;
        
        if (filter.getNutzerId() != null) {
            // Aktivitäten eines bestimmten Nutzers filtern
            aktivitaeten = aktivitaetsService.getFilteredAktivitaetenByNutzerId(
                    filter.getNutzerId(), 
                    filter.getAktivitaetsTypEnum(), 
                    filter.getSuchbegriff(),
                    filter.getPage(), 
                    filter.getSize(),
                    dateRangeSpec);
        } else {
            // Alle Aktivitäten filtern (systemweit)
            aktivitaeten = aktivitaetsService.getFilteredAktivitaeten(
                    filter.getAktivitaetsTypEnum(), 
                    filter.getSuchbegriff(),
                    filter.getPage(), 
                    filter.getSize(),
                    dateRangeSpec);
        }

        model.addAttribute("aktivitaeten", aktivitaeten);
        model.addAttribute("showAllUsers", filter.isShowAllUsers());
        model.addAttribute("nutzerId", filter.getNutzerId());
        model.addAttribute("showPagination", true);
        model.addAttribute("isKursbetreuer", isKursbetreuer);
        
        return "fragments/aktivitaeten-htmx :: aktivitaeten-content";
    }

}
