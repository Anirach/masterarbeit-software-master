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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller für HTMX-basierte Operationen an Kursmaterialien.
 * Dieser Controller stellt spezielle Endpunkte bereit, die insbesondere für
 * HTMX-Anfragen und partielle Seitenaktualisierungen optimiert sind.
 */
@Controller
@RequestMapping("/kursbetreuer/htmx")
@Slf4j
public class KursbetreuerMaterialHtmxController {

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
    public KursbetreuerMaterialHtmxController(
            KursMaterialService kursMaterialService,
            KursService kursService,
            KurseinheitService kurseinheitService) {
        this.kursMaterialService = kursMaterialService;
        this.kursService = kursService;
        this.kurseinheitService = kurseinheitService;
    }

    /**
     * Löscht ein Kursmaterial und gibt das aktualisierte Kursmaterial-Fragment zurück.
     * Diese Methode ist für HTMX-Anfragen optimiert und gibt nur das benötigte DOM-Fragment zurück.
     *
     * @param id Die ID des zu löschenden Kursmaterials
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit der Erfolgsmeldung und dem aktualisierten Materialbaum
     */
    @DeleteMapping("/kursmaterial/{id}")
    @ProtokolliereAktivitaet(aktivitaetsTyp = AktivitaetsTyp.KURSMATERIAL_LOESCHEN, beschreibung = "Material \"{0}\" gelöscht")
    public String deleteKursMaterial(@PathVariable("id") Long id, Model model) {
        try {
            // Name des Materials für die Erfolgs-Nachricht abrufen und prüfen, zu welchem Kurs/welcher Kurseinheit es gehört
            KursMaterialDTO material = kursMaterialService.getKursMaterialById(id);
            String materialName = material != null ? material.getName() : "Material";
            
            // Material löschen
            kursMaterialService.loescheKursMaterial(id);
            
            // Erfolgsmeldung zum Model hinzufügen (mit Fragment-spezifischem Key)
            model.addAttribute("fragmentSuccessMessage", "Das Material \"" + materialName + "\" wurde erfolgreich gelöscht.");
            
            // Wenn Material einem Kurs zugeordnet war, Kurs laden
            if (material != null && material.getKursId() != null) {
                KursDTO kurs = kursService.getKursByIdMitKurseinheiten(material.getKursId());
                model.addAttribute("kurs", kurs);
                model.addAttribute("aktivitaetsprotokollierung-arg-0", material.getName());
                return "fragments/kursbetreuer/kursmaterial-tree :: kursmaterial-tree";
            }
            // Wenn Material einer Kurseinheit zugeordnet war, Kurseinheit laden und Formular zurückgeben
            else if (material != null && material.getKurseinheitId() != null) {
                KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(material.getKurseinheitId());
                // Hier den übergeordneten Kurs laden, um den Tree darzustellen
                Long kursId = kurseinheit.getKursId();
                KursDTO kurs = kursService.getKursByIdMitKurseinheiten(kursId);
                model.addAttribute("kurs", kurs);
                // Den kompletten Tree zurückgeben statt nur die Liste
                return "fragments/kursbetreuer/kursmaterial-tree :: kursmaterial-tree";
            }
            
            // Falls keine Zuordnung gefunden wurde, nur Meldung zurückgeben
            return "fragments/messages :: successMessage";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler beim Löschen des Kursmaterials: " + e.getMessage());

            log.error("Fehler beim Löschen von Kursmaterial {}", id, e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }
    
    /**
     * Löscht ein Kursmaterial einer Kurseinheit und gibt das aktualisierte Kurseinheit-Material-Fragment zurück.
     * Diese Methode ist speziell für Anfragen aus der Kurseinheit-Ansicht konzipiert.
     *
     * @param id Die ID des zu löschenden Kursmaterials
     * @param model Das Model für die View
     * @return Ein Thymeleaf-Fragment mit der aktualisierten Materialliste der Kurseinheit
     */
    @DeleteMapping("/kurseinheit-material/{id}")
    @ProtokolliereAktivitaet(aktivitaetsTyp = AktivitaetsTyp.KURSMATERIAL_LOESCHEN, beschreibung = "Material \"{0}\" gelöscht")
    public String deleteKurseinheitMaterial(@PathVariable("id") Long id, Model model) {
        try {
            // Name des Materials für die Erfolgs-Nachricht abrufen
            KursMaterialDTO material = kursMaterialService.getKursMaterialById(id);
            String materialName = material != null ? material.getName() : "Material";
            Long kurseinheitId = material != null ? material.getKurseinheitId() : null;
            
            // Material löschen
            kursMaterialService.loescheKursMaterial(id);
            
            // Erfolgsmeldung zum Model hinzufügen (mit Fragment-spezifischem Key)
            model.addAttribute("fragmentSuccessMessage", "Das Material \"" + materialName + "\" wurde erfolgreich gelöscht.");
            model.addAttribute("aktivitaetsprotokollierung-arg-0", material.getName());

            // Wenn Material einer Kurseinheit zugeordnet war
            if (kurseinheitId != null) {
                KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
                model.addAttribute("kurseinheit", kurseinheit);
                return "fragments/kursbetreuer/kurseinheit-material-list :: kurseinheit-material-list";
            }
            
            // Falls keine Kurseinheit gefunden wurde, nur Meldung zurückgeben
            return "fragments/messages :: successMessage";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler beim Löschen des Kursmaterials: " + e.getMessage());

            log.error("Fehler beim Löschen von Kursmaterial {}", id, e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }
    

}
