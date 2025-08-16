package de.fuh.kn.webapp.aufgabenverwaltung.controller;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.service.AufgabeService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.LoesungsversuchService;
import de.fuh.kn.webapp.aufgabenverwaltung.service.TeilaufgabeService;
import de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsMapper;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsRequestDto;
import de.fuh.kn.webapp.llm.dto.bewertung.BewertungsResponseDto;
import de.fuh.kn.webapp.llm.dto.bewertung.FeedbackResponseDto;
import de.fuh.kn.webapp.llm.service.LlmBewertungService;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.Optional;

/**
 * HTMX-Controller für die Bearbeitung von Aufgaben durch Studenten.
 * Behandelt AJAX-Anfragen zur Bewertung von Lösungen.
 */
@Controller
@RequestMapping("/student/aufgaben/htmx")
public class StudentAufgabeHtmxController {

    private final NutzerService nutzerService;
    private final TeilaufgabeService teilaufgabeService;
    private final AufgabeService aufgabeService;
    private final AufgabenMarkdownService markdownService;
    private final LoesungsversuchService loesungsversuchService;
    private final LlmBewertungService llmBewertungService;
    private final BewertungsMapper bewertungsMapper;
    private final SpringTemplateEngine templateEngine;

    @Autowired
    public StudentAufgabeHtmxController(
            NutzerService nutzerService,
            TeilaufgabeService teilaufgabeService,
            AufgabeService aufgabeService,
            AufgabenMarkdownService markdownService,
            LoesungsversuchService loesungsversuchService,
            LlmBewertungService llmBewertungService,
            BewertungsMapper bewertungsMapper) {
        this.nutzerService = nutzerService;
        this.teilaufgabeService = teilaufgabeService;
        this.aufgabeService = aufgabeService;
        this.markdownService = markdownService;
        this.loesungsversuchService = loesungsversuchService;
        this.llmBewertungService = llmBewertungService;
        this.bewertungsMapper = bewertungsMapper;

        // Thymeleaf Template Engine für das Rendern des Markdowns
        this.templateEngine = new SpringTemplateEngine();
        org.thymeleaf.templateresolver.StringTemplateResolver templateResolver = new org.thymeleaf.templateresolver.StringTemplateResolver();
        templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        this.templateEngine.addTemplateResolver(templateResolver);
    }

    /**
     * Bewertet eine eingereichte Lösung mit dem LLM-Service.
     *
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param loesungFelder Die eingereichten Lösungen für die Felder
     * @param model Das Model für die View
     * @return Das Fragment mit der Bewertung
     */
    @PostMapping("/bewerten")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_BEWERTEN,
            beschreibung = "Lösung zur Bewertung eingereicht",
            mitParametern = true)
    public String bewerteLoesungsversuch(
            @RequestParam("teilaufgabeId") Long teilaufgabeId,
            @RequestParam Map<String, String> loesungFelder,
            Model model) {

        // Aktuelle Nutzerinformationen laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Teilaufgabe laden für Referenzierung in der Aktivität
        TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);

        // Lösungsfelder bereinigen (CSRF und andere Formular-Parameter entfernen)
        loesungFelder.remove("_csrf");
        loesungFelder.remove("teilaufgabeId");

        // Neuen Lösungsversuch erstellen und bewerten
        LoesungsVersuchDTO loesungsVersuch = loesungsversuchService.erstelleUndBewerteLoesungsversuch(
                studentDTO.getId(), teilaufgabeId, loesungFelder);

        // Aufgabe DTO für die Thymeleaf-Vorlage laden
        AufgabeDto aufgabeDto = aufgabeService.getAufgabeById(teilaufgabeDto.getAufgabeId());

        // Mock-Response für die Bewertung erstellen
        // In einer echten Implementation würde dieser Teil entfallen, da die Bewertung
        // bereits im LoesungsversuchService durchgeführt wird
        BewertungsResponseDto responseDto = BewertungsResponseDto.builder()
                .punkte(loesungsVersuch.getBewertungPunkte())
                .feedback(loesungsVersuch.getBewertungFeedback())
                .felderBewertung(loesungsVersuch.getBewertungFelderFarbe())
                .build();

        // Markdown neu rendern mit der STUDENT_INPUT-RenderMode
        MarkdownRenderResult teilaufgabeMarkdown = markdownService.renderMarkdownForStudentInput(
                teilaufgabeDto.getAufgabenstellungMarkdown(),
                aufgabeDto.getKurseinheitId());

        // Thymeleaf Kontext für das Rendering des Markdowns vorbereiten
        Context thymeleafContext = new Context();

        // Feld-Werte aus dem Lösungsversuch setzen
        for (InputFieldDto field : teilaufgabeMarkdown.getInputFields()) {
            String fieldName = field.getFieldName();
            String fieldValue = loesungsVersuch.getLoesungFelder().getOrDefault(fieldName, "");
            thymeleafContext.setVariable("field-" + fieldName, fieldValue);

            // Default-Werte für Lösungsfelder (werden nur im SOLUTION-Modus angezeigt)
            thymeleafContext.setVariable("field-" + fieldName + "-solution",
                    teilaufgabeDto.getMusterloesungFelder().getOrDefault(fieldName, ""));

            // Feld-Highlight-Klasse basierend auf der Bewertung
            String fieldClass = "";
            if (loesungsVersuch.getBewertungFelderFarbe() != null
                    && loesungsVersuch.getBewertungFelderFarbe().containsKey(fieldName)) {
                String color = loesungsVersuch.getBewertungFelderFarbe().get(fieldName);
                fieldClass = "field-highlight-" + color;
            }
            thymeleafContext.setVariable("field-" + fieldName + "-class", fieldClass);
        }

        // HTML durch Thymeleaf verarbeiten
        String processedHtml = templateEngine.process(
                "<div th:remove=\"tag\">" + teilaufgabeMarkdown.getHtml() + "</div>",
                thymeleafContext);

        // Ergebnis mit dem verarbeiteten HTML aktualisieren
        teilaufgabeMarkdown = new MarkdownRenderResult(
                processedHtml,
                teilaufgabeMarkdown.getInputFields());

        // Prüfen, ob der Student berechtigt ist, die Musterlösung zu sehen (mind. 50% erreicht)
        boolean kannMusterloesungSehen = loesungsversuchService.hatMindestens50ProzentErreicht(
                studentDTO.getId(), teilaufgabeId);

        // Wenn der Student die Musterlösung sehen darf, rendern wir sie mit dem SOLUTION RenderMode
        MarkdownRenderResult teilaufgabeMusterloesungMarkdown = null;
        if (kannMusterloesungSehen) {
            teilaufgabeMusterloesungMarkdown = markdownService.renderMarkdownForSolution(
                    teilaufgabeDto.getAufgabenstellungMarkdown(),
                    aufgabeDto.getKurseinheitId());

            // Thymeleaf-Kontext für das Rendern des Lösungsmarkdowns vorbereiten
            Context musterloesungContext = new Context();

            // Lösungswerte für alle Felder setzen
            for (InputFieldDto field : teilaufgabeMusterloesungMarkdown.getInputFields()) {
                String fieldName = field.getFieldName();
                String solutionValue = teilaufgabeDto.getMusterloesungFelder().getOrDefault(fieldName, "");

                // Werte für Thymeleaf setzen
                musterloesungContext.setVariable("field-" + fieldName, solutionValue);
                musterloesungContext.setVariable("field-" + fieldName + "-solution", solutionValue);
                musterloesungContext.setVariable("field-" + fieldName + "-class", "field-highlight-green");
            }

            // HTML durch Thymeleaf verarbeiten
            String processedHtmlSolution = templateEngine.process(
                    "<div th:remove=\"tag\">" + teilaufgabeMusterloesungMarkdown.getHtml() + "</div>",
                    musterloesungContext);

            // Ergebnis mit dem verarbeiteten HTML aktualisieren
            teilaufgabeMusterloesungMarkdown = new MarkdownRenderResult(
                    processedHtmlSolution,
                    teilaufgabeMusterloesungMarkdown.getInputFields());
        }

        // Daten für das Template hinzufügen
        model.addAttribute("bewertung", responseDto);
        model.addAttribute("loesungsVersuch", loesungsVersuch);
        model.addAttribute("aktiveTeilaufgabe", teilaufgabeDto);
        model.addAttribute("aufgabe", aufgabeDto);
        model.addAttribute("teilaufgabeMarkdown", teilaufgabeMarkdown);
        model.addAttribute("kannMusterloesungSehen", kannMusterloesungSehen);
        model.addAttribute("teilaufgabeMusterloesungMarkdown", teilaufgabeMusterloesungMarkdown);

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("aktivitaetsprotokollierung-arg-0", teilaufgabeDto);

        // Wichtig: Letzten Versuch explizit auf null setzen, damit die "Letzter Versuch"-Card verschwindet
        // wenn eine neue Bewertung angezeigt wird
        model.addAttribute("letzterVersuch", null);

        return "student/aufgabe/fragments/aufgaben-form-fragment :: aufgaben-form";
    }

    /**
     * Setzt alle Lösungsversuche für eine Teilaufgabe zurück.
     * Nach dem Zurücksetzen werden die Lösungsfelder nicht mehr vorausgefüllt.
     *
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Ein Redirect zur Aufgabenseite mit sauberen Eingabefeldern
     */
    @PostMapping("/zuruecksetzen")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_ZURUECKSETZEN,
            beschreibung = "Lösungsversuche zurückgesetzt")
    public String setzeLoesungsversucheZurueck(
            @RequestParam("teilaufgabeId") Long teilaufgabeId,
            Model model) {

        // Aktuelle Nutzerinformationen laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Teilaufgabe laden für Referenzierung in der Aktivität
        TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("aktivitaetsprotokollierung-arg-0", teilaufgabeDto);

        // Lösungsversuche zurücksetzen
        loesungsversuchService.setzeLoesungsversucheZurueck(studentDTO.getId(), teilaufgabeId);

        // Aufgabe ID für die Weiterleitung ermitteln
        Long aufgabeId = teilaufgabeDto.getAufgabeId();

        // Zur Aufgabenseite zurückleiten
        return "redirect:/student/aufgaben/" + aufgabeId;
    }

    /**
     * HTMX-Endpunkt, der die Lösungsversuche für eine Teilaufgabe lädt und als HTML zurückgibt.
     *
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param model Das Model für die View
     * @return Name der Fragment-Datei für HTMX
     */
    @RequestMapping("/loesungsversuche")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_VERSUCHE_ANZEIGEN,
            beschreibung = "Lösungsversuche angezeigt",
            mitParametern = true)
    public String getLösungsversuche(
            @RequestParam("teilaufgabeId") Long teilaufgabeId,
            Model model) {

        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Teilaufgabe laden
        TeilaufgabeDto teilaufgabe = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);
        if (teilaufgabe == null) {
            throw new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId);
        }

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("aktivitaetsprotokollierung-arg-0", teilaufgabe);

        // Alle Lösungsversuche des Studenten für diese Teilaufgabe laden
        java.util.List<LoesungsVersuchDTO> loesungsversuche = loesungsversuchService.findeAlleLösungsversuche(
                studentDTO.getId(), teilaufgabeId);

        // Daten an das Model übergeben
        model.addAttribute("teilaufgabe", teilaufgabe);
        model.addAttribute("loesungsversuche", loesungsversuche);

        // Fragment-Datei zurückgeben
        return "student/aufgabe/fragments/loesungsversuche-modal-fragment :: loesungsversuche-content";
    }

    /**
     * HTMX-Endpunkt, der Verbesserungsvorschläge für einen Lösungsversuch generiert.
     * Diese Funktion ist für Studenten gedacht, die zwischen 50 und 80 Punkten erreicht haben.
     *
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @param model Das Model für die View
     * @return Name der Fragment-Datei für HTMX
     */
    @GetMapping("/feedback")
    @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.AUFGABE_FEEDBACK,
            beschreibung = "Verbesserungsvorschläge angefordert",
            mitParametern = true)
    public String getVerbesserungsvorschlaege(
            @RequestParam("teilaufgabeId") Long teilaufgabeId,
            Model model) {

        // Aktuell angemeldeten Studenten laden
        StudentDTO studentDTO = nutzerService.getStudentById(nutzerService.getAuthenticatedNutzer().getId())
                .orElseThrow(() -> new IllegalStateException("Angemeldeter Nutzer ist kein Student"));

        // Teilaufgabe und Aufgabe laden
        TeilaufgabeDto teilaufgabeDto = teilaufgabeService.getTeilaufgabeById(teilaufgabeId);
        if (teilaufgabeDto == null) {
            throw new IllegalArgumentException("Teilaufgabe nicht gefunden: " + teilaufgabeId);
        }

        // Aktivitätsprotokollierung-Parameter für das Aspect setzen
        model.addAttribute("aktivitaetsprotokollierung-arg-0", teilaufgabeDto);

        AufgabeDto aufgabeDto = aufgabeService.getAufgabeById(teilaufgabeDto.getAufgabeId());

        // Neuesten Lösungsversuch des Studenten laden
        Optional<LoesungsVersuchDTO> neusterVersuch = loesungsversuchService.findeNeuesterLoesungsversuch(
                studentDTO.getId(), teilaufgabeId);

        if (neusterVersuch.isEmpty()) {
            throw new IllegalArgumentException("Kein Lösungsversuch gefunden für Teilaufgabe: " + teilaufgabeId);
        }

        // Bewertungsanfrage für das LLM erstellen
        BewertungsRequestDto requestDto = new BewertungsRequestDto();
        requestDto.setAufgabenstellungAufgabe(aufgabeDto.getAufgabenText());
        requestDto.setAufgabenstellungTeilaufgabe(teilaufgabeDto.getAufgabenstellungMarkdown());
        requestDto.setMusterloesungFelder(teilaufgabeDto.getMusterloesungFelder());
        requestDto.setLoesungFelder(neusterVersuch.get().getLoesungFelder());

        // Erreichte Punkte für die Berechnung des Feedbacks verwenden
        int punkte = neusterVersuch.get().getBewertungPunkte();

        // Verbesserungsvorschläge vom LLM generieren
        FeedbackResponseDto feedback = llmBewertungService.generateFeedback(requestDto, punkte);

        // Daten für das Template hinzufügen
        model.addAttribute("feedback", feedback);
        model.addAttribute("teilaufgabe", teilaufgabeDto);
        model.addAttribute("loesungsVersuch", neusterVersuch.get());

        // Fragment-Datei zurückgeben
        return "student/aufgabe/fragments/erlaeuterung-dialog-fragment :: feedback-content";
    }
}