package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.StudentAufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import de.fuh.kn.webapp.kursverwaltung.service.KursService;
import de.fuh.kn.webapp.kursverwaltung.service.KurseinheitService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller für die Bearbeitung von Aufgaben durch Studenten.
 */
@Controller
@RequestMapping("/student/aufgaben")
public class StudentAufgabeController {

    private final AufgabeService aufgabeService;
    private final NutzerService nutzerService;
    private final AufgabenMarkdownService markdownService;
    private final LoesungsversuchService loesungsversuchService;
    private final StudentAufgabeService studentAufgabeService;
    private final TeilaufgabeService teilaufgabeService;
    private final KurseinheitService kurseinheitService;
    private final KursService kursService;
    private final SpringTemplateEngine templateEngine;

    @Autowired
    public StudentAufgabeController(
            AufgabeService aufgabeService,
            NutzerService nutzerService,
            AufgabenMarkdownService markdownService,
            LoesungsversuchService loesungsversuchService,
            StudentAufgabeService studentAufgabeService,
            TeilaufgabeService teilaufgabeService,
            KurseinheitService kurseinheitService,
            KursService kursService) {
        this.aufgabeService = aufgabeService;
        this.nutzerService = nutzerService;
        this.markdownService = markdownService;
        this.loesungsversuchService = loesungsversuchService;
        this.studentAufgabeService = studentAufgabeService;
        this.teilaufgabeService = teilaufgabeService;
        this.kurseinheitService = kurseinheitService;
        this.kursService = kursService;

        this.templateEngine = new SpringTemplateEngine();

        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setOrder(100000);

        templateEngine.addTemplateResolver(templateResolver);
    }

    /**
     * Zeigt die Bearbeitungsansicht für eine Aufgabe.
     *
     * @param aufgabeId Die ID der Aufgabe
     * @param teilaufgabeId Die ID der Teilaufgabe (optional)
     * @param model Das Model für die View
     * @param redirectAttributes Attribute für Weiterleitungsnachrichten
     * @return Name der Template-Datei oder Weiterleitung
     */
    @GetMapping("/{aufgabeId}")
    public String showAufgabe(
            @PathVariable("aufgabeId") Long aufgabeId,
            @RequestParam(value = "teilaufgabe", required = false) Long teilaufgabeId,
            Model model,
            RedirectAttributes redirectAttributes) {
        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Aufgabe mit Teilaufgaben laden
        AufgabeDto aufgabe = aufgabeService.getAufgabeById(aufgabeId);
        if (aufgabe == null) {
            throw new IllegalArgumentException("Aufgabe nicht gefunden: " + aufgabeId);
        }

        // Zugriffsprüfung: Darf der Student diese Aufgabe bearbeiten?
        if (!studentAufgabeService.hatZugangZuAufgabe(aufgabeId, studentDTO.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Du musst zuerst alle vorherigen Aufgaben " +
                    "abschließen, bevor du diese Aufgabe bearbeiten kannst.");

            // Kurseinheit laden, um an die Kurs-ID zu gelangen
            KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabe.getKurseinheitId());
            if (kurseinheit == null) {
                throw new IllegalStateException("Kurseinheit nicht gefunden: " + aufgabe.getKurseinheitId());
            }

            return "redirect:/student/kurs/" + kurseinheit.getKursId();
        }

        // Aktuelle Teilaufgabe bestimmen
        TeilaufgabeDto aktiveTeilaufgabe = studentAufgabeService.ermittleAktiveTeilaufgabe(
                aufgabe, teilaufgabeId, studentDTO.getId());

        // Letzten Lösungsversuch laden, falls vorhanden und nicht zurückgesetzt
        Optional<LoesungsVersuchDTO> letzterVersuch = Optional.empty();
        if (aktiveTeilaufgabe != null) {
            letzterVersuch = loesungsversuchService.findeNeuesterLoesungsversuch(
                    studentDTO.getId(), aktiveTeilaufgabe.getId());
        }

        // Markdown rendern
        MarkdownRenderResult teilaufgabeMarkdown = null;
        if (aktiveTeilaufgabe != null) {
            // Render the markdown content using the student input mode
            teilaufgabeMarkdown = markdownService.renderMarkdownForStudentInput(
                    aktiveTeilaufgabe.getAufgabenstellungMarkdown(),
                    aufgabe.getKurseinheitId());

            // Process the HTML through Thymeleaf to resolve variables
            Context thymeleafContext = new Context();

            // Populate field values from the last attempt if available
            if (letzterVersuch.isPresent()) {
                LoesungsVersuchDTO versuch = letzterVersuch.get();
                for (InputFieldDto field : teilaufgabeMarkdown.getInputFields()) {
                    String fieldName = field.getFieldName();
                    String fieldValue = versuch.getLoesungFelder().getOrDefault(fieldName, "");
                    thymeleafContext.setVariable("field-" + fieldName, fieldValue);

                    // Add solution for answer fields (hidden by CSS)
                    thymeleafContext.setVariable("field-" + fieldName + "-solution",
                            aktiveTeilaufgabe.getMusterloesungFelder().getOrDefault(fieldName, ""));

                    // Add field highlight class based on the evaluation
                    String fieldClass = "";
                    if (versuch.getBewertungFelderFarbe() != null && versuch.getBewertungFelderFarbe().containsKey(fieldName)) {
                        String color = versuch.getBewertungFelderFarbe().get(fieldName);
                        fieldClass = "field-highlight-" + color;
                    }
                    thymeleafContext.setVariable("field-" + fieldName + "-class", fieldClass);
                }
            } else {
                // Initialize empty values for all fields
                for (InputFieldDto field : teilaufgabeMarkdown.getInputFields()) {
                    thymeleafContext.setVariable("field-" + field.getFieldName(), "");
                    thymeleafContext.setVariable("field-" + field.getFieldName() + "-solution", "");
                    thymeleafContext.setVariable("field-" + field.getFieldName() + "-class", "");
                }
            }

            // Process the HTML through Thymeleaf
            String processedHtml = templateEngine.process(
                    "<div th:remove=\"tag\">" + teilaufgabeMarkdown.getHtml() + "</div>",
                    thymeleafContext);

            // Update the markdown result with the processed HTML
            teilaufgabeMarkdown = new MarkdownRenderResult(
                    processedHtml,
                    teilaufgabeMarkdown.getInputFields());
        }

        // Allgemeinen Aufgabentext rendern, falls vorhanden
        MarkdownRenderResult allgemeinerAufgabentext = null;
        if (aufgabe.getAufgabenText() != null && !aufgabe.getAufgabenText().isBlank()) {
            allgemeinerAufgabentext = markdownService.renderMarkdownForStudentInput(
                    aufgabe.getAufgabenText(),
                    aufgabe.getKurseinheitId());

            // Process the HTML through Thymeleaf (for consistency)
            Context thymeleafContext = new Context();
            String processedHtml = templateEngine.process(
                    "<div th:remove=\"tag\">" + allgemeinerAufgabentext.getHtml() + "</div>",
                    thymeleafContext);

            // Update the markdown result with the processed HTML
            allgemeinerAufgabentext = new MarkdownRenderResult(
                    processedHtml,
                    allgemeinerAufgabentext.getInputFields());
        }

        // Prüfen, ob der Student berechtigt ist, die Musterlösung zu sehen (mind. 50% erreicht)
        boolean kannMusterloesungSehen = false;
        MarkdownRenderResult teilaufgabeMusterloesungMarkdown = null;

        if (aktiveTeilaufgabe != null) {
            kannMusterloesungSehen = loesungsversuchService.hatMindestens50ProzentErreicht(
                    studentDTO.getId(), aktiveTeilaufgabe.getId());

            // Wenn der Student die Musterlösung sehen darf, rendern wir sie mit dem SOLUTION RenderMode
            if (kannMusterloesungSehen) {
                teilaufgabeMusterloesungMarkdown = markdownService.renderMarkdownForSolution(
                        aktiveTeilaufgabe.getAufgabenstellungMarkdown(),
                        aufgabe.getKurseinheitId());

                // Thymeleaf-Kontext für das Rendern des Lösungsmarkdowns vorbereiten
                Context musterloesungContext = new Context();

                // Lösungswerte für alle Felder setzen
                for (InputFieldDto field : teilaufgabeMusterloesungMarkdown.getInputFields()) {
                    String fieldName = field.getFieldName();
                    String solutionValue = aktiveTeilaufgabe.getMusterloesungFelder().getOrDefault(fieldName, "");

                    // Werte für Thymeleaf setzen
                    musterloesungContext.setVariable("field-" + fieldName, solutionValue);
                    musterloesungContext.setVariable("field-" + fieldName + "-solution", solutionValue);
                    musterloesungContext.setVariable("field-" + fieldName + "-class", "field-highlight-green");
                }

                // HTML durch Thymeleaf verarbeiten
                String processedHtml = templateEngine.process(
                        "<div th:remove=\"tag\">" + teilaufgabeMusterloesungMarkdown.getHtml() + "</div>",
                        musterloesungContext);

                // Ergebnis mit dem verarbeiteten HTML aktualisieren
                teilaufgabeMusterloesungMarkdown = new MarkdownRenderResult(
                        processedHtml,
                        teilaufgabeMusterloesungMarkdown.getInputFields());
            }
        }

        // Für die Teilaufgaben-Fortschrittsanzeige alle Lösungsversuche laden
        List<Long> alleTeilaufgabeIds = aufgabe.getTeilaufgaben().stream()
                .map(TeilaufgabeDto::getId)
                .toList();
        Map<Long, LoesungsVersuchDTO> loesungsVersuche =
                loesungsversuchService.findeNeuesteLoesungsversucheFuerAlleTeilaufgaben(studentDTO.getId(), alleTeilaufgabeIds);

        // Kurseinheit laden, um an die Kurs-ID zu gelangen
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabe.getKurseinheitId());
        if (kurseinheit == null) {
            throw new IllegalStateException("Kurseinheit nicht gefunden: " + aufgabe.getKurseinheitId());
        }

        // Kurs laden, um den Namen für die Breadcrumb zu erhalten
        KursDTO kurs = kursService.getKursById(kurseinheit.getKursId());
        if (kurs == null) {
            throw new IllegalStateException("Kurs nicht gefunden: " + kurseinheit.getKursId());
        }

        // Daten an das Model übergeben
        model.addAttribute("aufgabe", aufgabe);
        model.addAttribute("aktiveTeilaufgabe", aktiveTeilaufgabe);
        model.addAttribute("teilaufgabeMarkdown", teilaufgabeMarkdown);
        model.addAttribute("allgemeinerAufgabentext", allgemeinerAufgabentext);
        model.addAttribute("letzterVersuch", letzterVersuch.orElse(null));
        model.addAttribute("student", studentDTO);
        model.addAttribute("kannMusterloesungSehen", kannMusterloesungSehen);
        model.addAttribute("teilaufgabeMusterloesungMarkdown", teilaufgabeMusterloesungMarkdown);
        model.addAttribute("loesungsVersuche", loesungsVersuche);
        model.addAttribute("kursId", kurseinheit.getKursId());
        model.addAttribute("kurs", kurs);

        return "student/aufgabe/aufgabe-bearbeiten";
    }

    /**
     * Markiert einen Lösungsversuch als abgeschlossen.
     *
     * @param loesungsversuchId Die ID des Lösungsversuchs
     * @param redirectAttributes Redirect-Attribute für Feedback-Nachrichten
     * @return Weiterleitung zur nächsten Teilaufgabe oder zurück zum Kurs
     */
    @PostMapping("/abschliessen")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ABSCHLIESSEN,
            beschreibung = "Teilaufgabe abgeschlossen",
            mitParametern = true)
    public String markiereLoesungsversuchAbgeschlossen(
            @RequestParam("loesungsversuchId") Long loesungsversuchId,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Lösungsversuch laden und als abgeschlossen markieren
        LoesungsVersuchDTO loesungsVersuch = loesungsversuchService.markiereLoesungsversuchAbgeschlossen(
                loesungsversuchId, studentDTO.getId());

        // Aufgabe und Teilaufgabe ermitteln
        Long teilaufgabeId = loesungsVersuch.getTeilaufgabeId();
        TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("teilaufgabe", teilaufgabeDto);

        AufgabeDto aufgabeDto = aufgabeService.getAufgabeByTeilaufgabeId(teilaufgabeId);
        Long aufgabeId = aufgabeDto.getId();

        // Erfolgsmeldung hinzufügen
        redirectAttributes.addFlashAttribute("successMessage",
                "Teilaufgabe erfolgreich abgeschlossen! Punkte: " + loesungsVersuch.getBewertungPunkte() + "/100");

        // Nächste nicht abgeschlossene Teilaufgabe suchen
        Optional<TeilaufgabeDto> naechsteTeilaufgabe = studentAufgabeService.findeNaechsteTeilaufgabe(
                aufgabeDto, teilaufgabeId, studentDTO.getId());

        // Wenn es eine nächste Teilaufgabe gibt, zu dieser navigieren
        if (naechsteTeilaufgabe.isPresent()) {
            return "redirect:/student/aufgaben/" + aufgabeId + "?teilaufgabe=" + naechsteTeilaufgabe.get().getId();
        }

        // Wenn keine weiteren Teilaufgaben oder alle abgeschlossen sind, Erfolgsmeldung anzeigen
        redirectAttributes.addFlashAttribute("successMessage",
                "Aufgabe erfolgreich abgeschlossen! Alle Teilaufgaben wurden bearbeitet.");

        // Kurseinheit laden, um an die Kurs-ID zu gelangen
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabeDto.getKurseinheitId());
        if (kurseinheit == null) {
            throw new IllegalStateException("Kurseinheit nicht gefunden: " + aufgabeDto.getKurseinheitId());
        }

        // Zurück zum Kurs, wenn alle Teilaufgaben abgeschlossen sind
        return "redirect:/student/kurs/" + kurseinheit.getKursId();
    }

    /**
     * Ermöglicht das Überspringen einer Teilaufgabe.
     * Diese Funktion erstellt einen neuen Lösungsversuch, der als übersprungen markiert ist,
     * sodass der Student zur nächsten Aufgabe weitergehen kann, auch wenn die aktuelle
     * Aufgabe nicht erfolgreich gelöst wurde.
     *
     * @param teilaufgabeId Die ID der zu überspringenden Teilaufgabe
     * @param redirectAttributes Redirect-Attribute für Feedback-Nachrichten
     * @return Weiterleitung zur nächsten Teilaufgabe oder zurück zum Kurs
     */
    @PostMapping("/ueberspringen")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ABSCHLIESSEN,
            beschreibung = "Teilaufgabe übersprungen")
    public String ueberspringe(
            @RequestParam("teilaufgabeId") Long teilaufgabeId,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Aktuelle Nutzerinformationen laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Teilaufgabe laden für Referenzierung in der Aktivität
        TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("teilaufgabe", teilaufgabeDto);

        // Lösungsversuch erstellen, der als übersprungen markiert ist
        LoesungsVersuchDTO loesungsVersuch = loesungsversuchService.erstelleUebersprungenenLoesungsversuch(
                studentDTO.getId(), teilaufgabeId);

        // Aufgabe ermitteln
        AufgabeDto aufgabeDto = aufgabeService.getAufgabeByTeilaufgabeId(teilaufgabeId);
        Long aufgabeId = aufgabeDto.getId();

        // Hinweis anzeigen
        redirectAttributes.addFlashAttribute("warningMessage",
                "Du hast diese Teilaufgabe übersprungen. Du kannst jederzeit zurückkehren, um sie zu lösen.");

        // Nächste nicht abgeschlossene Teilaufgabe suchen
        Optional<TeilaufgabeDto> naechsteTeilaufgabe = studentAufgabeService.findeNaechsteTeilaufgabe(
                aufgabeDto, teilaufgabeId, studentDTO.getId());

        // Wenn es eine nächste Teilaufgabe gibt, zu dieser navigieren
        if (naechsteTeilaufgabe.isPresent()) {
            return "redirect:/student/aufgaben/" + aufgabeId + "?teilaufgabe=" + naechsteTeilaufgabe.get().getId();
        }

        // Wenn alle Teilaufgaben erledigt sind (abgeschlossen oder übersprungen)
        redirectAttributes.addFlashAttribute("warningMessage",
                "Du hast alle Teilaufgaben dieser Aufgabe bearbeitet oder übersprungen.");

        // Kurseinheit laden, um an die Kurs-ID zu gelangen
        KurseinheitDTO kurseinheit = kurseinheitService.getKurseinheitById(aufgabeDto.getKurseinheitId());
        if (kurseinheit == null) {
            throw new IllegalStateException("Kurseinheit nicht gefunden: " + aufgabeDto.getKurseinheitId());
        }

        // Zurück zum Kurs
        return "redirect:/student/kurs/" + kurseinheit.getKursId();
    }
}