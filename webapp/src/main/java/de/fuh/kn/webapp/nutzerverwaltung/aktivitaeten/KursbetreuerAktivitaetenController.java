package de.fuh.kn.webapp.nutzerverwaltung.aktivitaeten;

import de.fuh.kn.webapp.common.aktivitaeten.AktivitaetsService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller für die Anzeige und Verwaltung von Aktivitäten durch Kursbetreuer.
 * Ermöglicht Kursbetreuern, alle Systemaktivitäten einzusehen.
 */
@Controller
@RequestMapping("/kursbetreuer/aktivitaeten")
public class KursbetreuerAktivitaetenController {

    private final AktivitaetsService aktivitaetsService;

    @Autowired
    public KursbetreuerAktivitaetenController(AktivitaetsService aktivitaetsService) {
        this.aktivitaetsService = aktivitaetsService;
    }

    /**
     * Zeigt die Übersichtsseite für alle Aktivitäten im System an.
     * Diese Ansicht ist nur für Kursbetreuer zugänglich.
     *
     * @param filter Der Filter für Aktivitäten (optional, wird automatisch initialisiert)
     * @param model Spring MVC Model
     * @return Die View für die Aktivitätenübersicht
     */
    @GetMapping
    public String zeigeAlleAktivitaeten(@ModelAttribute AktivitaetFilterDTO filter, Model model) {
        // Wenn kein Filter übergeben wurde, Standards setzen
        if (filter == null) {
            filter = new AktivitaetFilterDTO();
            filter.setShowAllUsers(true);
            filter.setPage(0);
            filter.setSize(10);
        }
        
        // Aktivitäten laden (ohne inhaltliche Filter, nur Paginierung)
        Page<AktivitaetDTO> aktivitaeten = aktivitaetsService.getAlleAktivitaeten(filter.getPage(), filter.getSize());
        
        // Model-Attribute setzen
        model.addAttribute("aktivitaeten", aktivitaeten);
        model.addAttribute("showAllUsers", true);
        model.addAttribute("nutzerId", null);
        model.addAttribute("filter", filter);
        model.addAttribute("isKursbetreuer", true);

        // Template-Name zurückgeben
        return "kursbetreuer/aktivitaeten/aktivitaeten";
    }

}
