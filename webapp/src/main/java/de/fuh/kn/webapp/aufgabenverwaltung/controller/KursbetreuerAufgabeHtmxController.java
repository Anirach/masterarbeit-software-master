package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMaterialDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursMaterialService;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.llm.service.PromptPreviewService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller für HTMX-basierte Operationen für Aufgaben und Teilaufgaben.
 * Dieser Controller stellt spezielle Endpunkte bereit, die insbesondere für
 * HTMX-Anfragen und partielle Seitenaktualisierungen optimiert sind.
 */
@Controller
@RequestMapping("/kursbetreuer/htmx")
@Slf4j
public class KursbetreuerAufgabeHtmxController {

    private final AufgabeService aufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final AufgabenMarkdownService aufgabenMarkdownService;
    private final KurseinheitService kurseinheitService;
    private final KursService kursService;
    private final KursMaterialService kursMaterialService;
    private final PromptPreviewService promptPreviewService;

    /**
     * Konstruktor mit Dependency Injection der benötigten Services.
     *
     * @param aufgabeService          Der Service für die Verwaltung von Aufgaben
     * @param teilaufgabeService      Der Service für die Verwaltung von Teilaufgaben
     * @param aufgabenMarkdownService Der Service für die Markdown-Verarbeitung
     * @param kurseinheitService      Der Service für die Verwaltung von Kurseinheiten
     * @param kursService             Der Service für die Verwaltung von Kursen
     * @param kursMaterialService     Der Service für die Verwaltung von Kursmaterialien
     * @param promptPreviewService    Der Service für die Generierung von Prompt-Vorschauen
     */
    @Autowired
    public KursbetreuerAufgabeHtmxController(
            AufgabeService aufgabeService,
            TeilaufgabeService teilaufgabeService,
            AufgabenMarkdownService aufgabenMarkdownService,
            KurseinheitService kurseinheitService,
            KursService kursService,
            KursMaterialService kursMaterialService,
            PromptPreviewService promptPreviewService) {
        this.aufgabeService = aufgabeService;
        this.teilaufgabeService = teilaufgabeService;
        this.aufgabenMarkdownService = aufgabenMarkdownService;
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;
        this.kursMaterialService = kursMaterialService;
        this.promptPreviewService = promptPreviewService;
    }

    /**
     * Aktualisiert die Reihenfolge der Aufgaben einer Kurseinheit per Drag & Drop.
     * Diese Methode nimmt eine Liste von Aufgaben-IDs in der neuen Reihenfolge entgegen und aktualisiert die Datenbank.
     *
     * @param kurseinheitId Die ID der Kurseinheit, deren Aufgaben neu sortiert werden
     * @param ids           Liste der Aufgaben-IDs in der neuen Reihenfolge
     * @param model         Das Model für die View
     * @return Ein Thymeleaf-Fragment mit der aktualisierten Aufgabenliste und einer Erfolgsmeldung
     */
    @PostMapping("/aufgaben/{kurseinheitId}/reorder")
    public String reorderAufgaben(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam List<Long> ids,
            Model model) {
        try {
            // Prüfen, ob IDs vorhanden sind
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("Keine Aufgaben-IDs erhalten");
            }

            // Reihenfolge aktualisieren
            List<AufgabeDto> aufgaben = aufgabeService.aktualisiereAufgabenReihenfolge(kurseinheitId, ids);

            // Aktualisierte Aufgaben zum Model hinzufügen
            model.addAttribute("aufgaben", aufgaben);
            model.addAttribute("kurseinheitId", kurseinheitId);

            // Erfolgsmeldung setzen
            model.addAttribute("successMessage", "Die Reihenfolge der Aufgaben wurde erfolgreich aktualisiert.");

            // Fragment mit der Aufgabenliste zurückgeben
            return "fragments/kursbetreuer/aufgaben-liste-fragment :: aufgaben-liste-fragment";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler bei der Aktualisierung der Reihenfolge: " + e.getMessage());

            log.error("Fehler beim Umsortieren von Aufgaben", e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }

    /**
     * Generiert eine Live-Vorschau des Markdown-Texts.
     * Diese Methode nimmt den eingegebenen Markdown-Text und eine KurseinheitId entgegen und gibt das gerenderte HTML zurück.
     *
     * @param markdown Der zu rendernde Markdown-Text
     * @param kurseinheitId Die ID der Kurseinheit für die Bildverarbeitung
     * @return Ein ResponseEntity mit dem gerenderten HTML und den extrahierten Input-Feldern als JSON
     */
    @PostMapping("/markdown-preview")
    @ResponseBody
    public ResponseEntity<?> markdownPreview(
            @RequestBody(required = false) String markdown, 
            @RequestParam Long kurseinheitId) {
        try {
            // Wenn markdown null oder leer ist, ein leeres Ergebnis zurückgeben
            if (markdown == null || markdown.isEmpty()) {
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("outputHtml", "");
                emptyResponse.put("inputFields", new ArrayList<>());
                return ResponseEntity.ok(emptyResponse);
            }
            
            // Markdown rendern für die Vorschau nur wenn der Text nicht leer ist
            MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForPreview(markdown, kurseinheitId);
            
            // Überprüfen ob das Ergebnis null ist und ggf. ein leeres Ergebnis zurückgeben
            if (result == null) {
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("outputHtml", "");
                emptyResponse.put("inputFields", new ArrayList<>());
                return ResponseEntity.ok(emptyResponse);
            }
            
            // Erstelle eine Map für die JSON-Antwort
            Map<String, Object> response = new HashMap<>();
            response.put("outputHtml", result.getHtml());
            response.put("inputFields", result.getInputFields());
            
            // Gebe das Ergebnis als JSON zurück
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fehler bei der Markdown-Vorschau", e);
            
            // Fehlermeldung als JSON zurückgeben
            Map<String, String> error = new HashMap<>();
            error.put("error", "Fehler bei der Markdown-Vorschau: " + e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Fügt dynamisch eine neue leere Teilaufgabe zum Formular hinzu.
     * Diese Methode verwendet das gesamte Formular-Model und ersetzt das gesamte Formular mit der aktualisierten Version.
     * Die neue Teilaufgabe wird sofort in der Datenbank gespeichert, um ein gültiges ID zu haben.
     *
     * @param aufgabe Die aktuelle AufgabeDto aus dem Formular
     * @param model Das Model für die View
     * @return Das aktualisierte Formular mit der neuen Teilaufgabe
     */
    @PostMapping("/aufgabe/add-teilaufgabe")
    public String addTeilaufgabe(@ModelAttribute("aufgabe") AufgabeDto aufgabe, Model model) {
        try {
            // Neue leere Teilaufgabe erstellen
            TeilaufgabeDto teilaufgabe = new TeilaufgabeDto();
            teilaufgabe.setAufgabenstellungMarkdown(""); // Leere Aufgabenstellung setzen
            teilaufgabe.setMusterloesungFelder(new HashMap<>());
            teilaufgabe.setMusterloesungBewertungshinweise("");
            teilaufgabe.setAufgabeId(aufgabe.getId());
            
            // Hinzufügen der neuen Teilaufgabe zur bestehenden Liste
            List<TeilaufgabeDto> teilaufgaben = aufgabe.getTeilaufgaben();
            if (teilaufgaben == null) {
                teilaufgaben = new ArrayList<>();
            }
            
            // Fortlaufende Nummer zuweisen
            int maxReihenfolge = 0;
            if (!teilaufgaben.isEmpty()) {
                maxReihenfolge = teilaufgaben.stream()
                    .mapToInt(TeilaufgabeDto::getReihenfolge)
                    .max()
                    .orElse(0);
            }
            teilaufgabe.setReihenfolge(maxReihenfolge + 1);

            // Speichere die Teilaufgabe sofort in der Datenbank, wenn die Aufgabe bereits gespeichert wurde
            if (aufgabe.getId() != null) {
                teilaufgabe = teilaufgabeService.erstelleTeilaufgabe(teilaufgabe);
            }

            teilaufgaben.add(teilaufgabe);
            aufgabe.setTeilaufgaben(teilaufgaben);
            
            // Einfach-Flag basierend auf der Anzahl der Teilaufgaben aktualisieren
            aufgabe.setEinfach(teilaufgaben.size() <= 1);
            
            // Notwendige Attribute zum Model hinzufügen
            model.addAttribute("aufgabe", aufgabe);
            model.addAttribute("isNew", aufgabe.getId() == null);
            
            // Wir müssen kurseinheit und kurs zum Model hinzufügen
            Long kurseinheitId = aufgabe.getKurseinheitId();
            if (kurseinheitId != null) {
                KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
                model.addAttribute("kurseinheit", kurseinheit);
                
                if (kurseinheit != null) {
                    KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
                    model.addAttribute("kurs", kurs);
                }
            }
            
            // Das komplette Formular zurückgeben
            return "kursbetreuer/aufgabe/aufgabe-bearbeiten :: form-aufgabe";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler beim Hinzufügen einer neuen Teilaufgabe: " + e.getMessage());

            log.error("Fehler beim Hinzufügen einer neuen Teilaufgabe", e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }

    /**
     * Entfernt dynamisch eine Teilaufgabe aus dem Formular.
     * Diese Methode wird verwendet, um eine Teilaufgabe aus dem Bearbeitungsformular zu entfernen.
     *
     * @param id    Die ID der zu entfernenden Teilaufgabe
     * @param model Das Model für die View
     * @return Ein leeres Fragment (die Teilaufgabe wird client-seitig entfernt)
     */
    @DeleteMapping("/aufgabe/remove-teilaufgabe")
    public String removeTeilaufgabe(
            @RequestParam("id") Long id,
            Model model) {
        try {

            TeilaufgabeDto teilaufgabe = teilaufgabeService.getTeilaufgabeById(id);
            if (id > 0) {
                teilaufgabeService.loescheTeilaufgabe(id);
            }

            AufgabeDto aufgabe = aufgabeService.getAufgabeById(teilaufgabe.getAufgabeId());

            // Erfolgsmeldung setzen
            model.addAttribute("successMessage", "Teilaufgabe wurde erfolgreich entfernt.");

            // Notwendige Attribute zum Model hinzufügen
            model.addAttribute("aufgabe", aufgabe);
            model.addAttribute("isNew", aufgabe.getId() == null);

            // Wir müssen kurseinheit und kurs zum Model hinzufügen
            Long kurseinheitId = aufgabe.getKurseinheitId();
            if (kurseinheitId != null) {
                KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
                model.addAttribute("kurseinheit", kurseinheit);

                if (kurseinheit != null) {
                    KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
                    model.addAttribute("kurs", kurs);
                }
            }

            // Das komplette Formular zurückgeben
            return "kursbetreuer/aufgabe/aufgabe-bearbeiten :: form-aufgabe";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler beim Entfernen der Teilaufgabe: " + e.getMessage());

            log.error("Fehler beim Entfernen einer Teilaufgabe", e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }
    
    /**
     * Aktualisiert die Reihenfolge der Teilaufgaben per Drag & Drop.
     * Diese Methode nimmt eine Liste von Teilaufgaben-IDs in der neuen Reihenfolge entgegen und
     * aktualisiert die entsprechenden Reihenfolgenwerte in der Datenbank.
     *
     * @param aufgabeId Die ID der Aufgabe, deren Teilaufgaben neu sortiert werden
     * @param ids       Liste der Teilaufgaben-IDs in der neuen Reihenfolge
     * @param model     Das Model für die View
     * @return Eine Erfolgsmeldung oder Fehlermeldung
     */
    @PostMapping("/teilaufgabe/{aufgabeId}/reorder")
    public String reorderTeilaufgaben(
            @PathVariable("aufgabeId") Long aufgabeId,
            @RequestParam List<Long> ids,
            @ModelAttribute("aufgabe") AufgabeDto aufgabe,
            Model model) {
        try {
            // Prüfen, ob IDs vorhanden sind
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("Keine Teilaufgaben-IDs erhalten");
            }

            // Reihenfolge aktualisieren

            for (TeilaufgabeDto teilaufgabeDto : aufgabe.getTeilaufgaben()) {
                int index = ids.indexOf(teilaufgabeDto.getId());
                teilaufgabeDto.setReihenfolge(index+1);
            }
            aufgabe.getTeilaufgaben().sort(Comparator.comparing(TeilaufgabeDto::getReihenfolge));

            // Erfolgsmeldung setzen
            model.addAttribute("successMessage", "Die Reihenfolge der Teilaufgaben wurde erfolgreich aktualisiert.");

            // Notwendige Attribute zum Model hinzufügen
            model.addAttribute("aufgabe", aufgabe);

            // Wir müssen kurseinheit und kurs zum Model hinzufügen
            Long kurseinheitId = aufgabe.getKurseinheitId();
            if (kurseinheitId != null) {
                KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
                model.addAttribute("kurseinheit", kurseinheit);

                if (kurseinheit != null) {
                    KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
                    model.addAttribute("kurs", kurs);
                }
            }

            // Das komplette Formular zurückgeben
            return "kursbetreuer/aufgabe/aufgabe-bearbeiten :: form-aufgabe";
        } catch (Exception e) {
            // Fehlermeldung zum Model hinzufügen
            model.addAttribute("errorMessage", "Fehler bei der Aktualisierung der Reihenfolge: " + e.getMessage());

            log.error("Fehler beim Umsortieren von Teilaufgaben", e);

            // Fragment mit der Fehlermeldung zurückgeben
            return "fragments/messages :: errorMessage";
        }
    }

    /**
     * Liefert das Modal-Fragment für die Bildauswahl.
     * Diese Methode wird vom Markdown-Editor verwendet, um das Modal zur Bildauswahl anzuzeigen.
     *
     * @param kurseinheitId Die ID der Kurseinheit
     * @param editorId Die ID des Editors, in den das Bild eingefügt werden soll
     * @param model Das Model für die View
     * @return Fragment mit dem Inhalt des Modals
     */
    @GetMapping("/image-selector-modal/{kurseinheitId}")
    public String getImageSelectorModal(
            @PathVariable("kurseinheitId") Long kurseinheitId,
            @RequestParam("editorId") String editorId,
            Model model) {
        try {
            // Kurseinheit abrufen
            KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(kurseinheitId);
            if (kurseinheit == null) {
                model.addAttribute("errorMessage", "Kurseinheit nicht gefunden");
                return "fragments/messages :: errorMessage";
            }
            
            // Bilder über den Service abrufen
            List<KursMaterialDTO> bilder = kursMaterialService.findAllBilderForMarkdownEditor(kurseinheitId);
            
            // Bilder für die View vorbereiten
            List<Map<String, Object>> imagesList = new ArrayList<>();
            
            for (KursMaterialDTO bild : bilder) {
                Map<String, Object> imageMap = new HashMap<>();
                imageMap.put("id", bild.getId());
                imageMap.put("name", bild.getName());
                
                // Source bestimmen basierend auf den gesetzten IDs
                if (bild.getKurseinheitId() != null) {
                    imageMap.put("source", "kurseinheit");
                } else if (bild.getKursId() != null) {
                    imageMap.put("source", "kurs");
                } else {
                    // Fallback, sollte nicht vorkommen
                    imageMap.put("source", "unbekannt");
                }
                
                imageMap.put("thumbnailUrl", "/material/kurseinheit/" + kurseinheitId + "/bild?name=" + bild.getName());
                imagesList.add(imageMap);
            }
            
            // Attribute zum Model hinzufügen
            model.addAttribute("kurseinheitId", kurseinheitId);
            model.addAttribute("images", imagesList);
            model.addAttribute("editorId", editorId);
            
            // Fragment zurückgeben
            return "fragments/components/markdown-editor :: image-selector-modal-content";
            
        } catch (Exception e) {
            log.error("Fehler beim Laden des Bildauswahl-Modals", e);
            model.addAttribute("errorMessage", "Fehler beim Laden des Bildauswahl-Modals: " + e.getMessage());
            return "fragments/messages :: errorMessage";
        }
    }
    
    /**
     * Generiert eine Vorschau des Prompts, der zur Bewertung verwendet wird.
     * Diese Methode nimmt die IDs von Aufgabe und Teilaufgabe entgegen und
     * generiert den entsprechenden Bewertungsprompt.
     * Die Methode verarbeitet auch die aktuellen Formularwerte, um eine Echtzeit-Vorschau zu generieren.
     *
     * @param aufgabeId Die ID der Aufgabe
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param aufgabe Die aktuelle Aufgabe aus dem Formular
     * @param model Das Model für die View
     * @return Ein Fragment mit der Prompt-Vorschau
     */
    @PostMapping("/prompt-preview/{aufgabeId}/{teilaufgabeId}")
    public String getPromptPreview(
            @PathVariable("aufgabeId") Long aufgabeId,
            @PathVariable("teilaufgabeId") Long teilaufgabeId,
            @ModelAttribute("aufgabe") AufgabeDto aufgabe,
            Model model) {
        try {
            // Falls die Aufgabe aus dem Formular null ist, laden wir sie aus der Datenbank
            if (aufgabe == null) {
                aufgabe = aufgabeService.getAufgabeById(aufgabeId);
                if (aufgabe == null) {
                    model.addAttribute("errorMessage", "Aufgabe nicht gefunden");
                    return "fragments/messages :: errorMessage";
                }
            }
            
            // Teilaufgabe finden
            TeilaufgabeDto teilaufgabe = null;
            for (TeilaufgabeDto ta : aufgabe.getTeilaufgaben()) {
                if (ta.getId().equals(teilaufgabeId)) {
                    teilaufgabe = ta;
                    break;
                }
            }
            
            if (teilaufgabe == null) {
                model.addAttribute("errorMessage", "Teilaufgabe nicht gefunden");
                return "fragments/messages :: errorMessage";
            }
            
            // Prompt-Vorschau generieren mit den aktuellen Formularwerten
            String promptPreview = promptPreviewService.generateEvaluationPromptPreview(aufgabe, teilaufgabe);
            
            // Vorschau zum Model hinzufügen
            model.addAttribute("promptPreview", promptPreview);
            model.addAttribute("teilaufgabeId", teilaufgabeId);
            
            // Fragment zurückgeben
            return "fragments/kursbetreuer/prompt-preview-fragment :: prompt-preview-fragment";
            
        } catch (Exception e) {
            log.error("Fehler bei der Generierung der Prompt-Vorschau", e);
            model.addAttribute("errorMessage", "Fehler bei der Generierung der Prompt-Vorschau: " + e.getMessage());
            return "fragments/messages :: errorMessage";
        }
    }
    
    /**
     * GET-Endpunkt für die Prompt-Vorschau (Redirect zu POST, um mit dem alten Endpunkt kompatibel zu bleiben).
     */
    @GetMapping("/prompt-preview/{aufgabeId}/{teilaufgabeId}")
    public String getPromptPreviewGet(
            @PathVariable("aufgabeId") Long aufgabeId,
            @PathVariable("teilaufgabeId") Long teilaufgabeId,
            Model model) {
        // Aufgabe aus der Datenbank laden
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(aufgabeId);
        return getPromptPreview(aufgabeId, teilaufgabeId, aufgabe, model);
    }
}