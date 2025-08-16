package de.fuh.kn.webapp.llm.service;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import de.fuh.kn.webapp.common.markdown.AufgabenMarkdownService;
import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.llm.prompt.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service zum Generieren von Prompt-Vorschauen für die Kursbetreuer.
 * Ermöglicht es den Kursbetreuern, zu sehen, wie der Prompt für die LLM-Bewertung aussehen wird.
 */
@Service
@RequiredArgsConstructor
public class PromptPreviewService {

    private final AufgabenMarkdownService aufgabenMarkdownService;

    /**
     * Erzeugt eine Vorschau des Bewertungsprompts für eine Teilaufgabe.
     * 
     * @param aufgabeDto Die Hauptaufgabe
     * @param teilaufgabeDto Die Teilaufgabe
     * @return Der vollständig formatierte Prompt als String
     */
    public String generateEvaluationPromptPreview(AufgabeDto aufgabeDto, TeilaufgabeDto teilaufgabeDto) {
        PromptTemplate template = new PromptTemplate(PromptTemplates.EVALUATION_TEMPLATE);
        
        // Format-String für die Bean-Ausgabe erzeugen (Platzhalter)
        String formatPlaceholder = """
            Strukturiere deine Antwort als JSON-Objekt mit den folgenden Keys:
            {
                "punkte": <punktwert 0-100>,
                "feedback": "<dein_feedback_text>",
                "feldfarben": {
                    "<feldname1>": "<farbe>",
                    "<feldname2>": "<farbe>",
                    ...
                }
            }
            """;
        
        // Kombinierter Aufgabentext für die Markdownrenderer
        String combinedMarkdown = (aufgabeDto.getAufgabenText() != null ? aufgabeDto.getAufgabenText() : "") + "\n" + 
                                 teilaufgabeDto.getAufgabenstellungMarkdown();
        
        // Extrahiere aktuelle Input-Felder aus dem Markdown
        List<InputFieldDto> actualFields = extractActualFields(combinedMarkdown);
        Set<String> actualFieldNames = actualFields.stream()
                                                  .map(InputFieldDto::getFieldName)
                                                  .collect(Collectors.toSet());
        
        // Filtere Musterloesungsfelder auf die tatsächlich vorhandenen Felder
        Map<String, String> filteredMusterloesungFelder = teilaufgabeDto.getMusterloesungFelder().entrySet().stream()
                .filter(entry -> actualFieldNames.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // Musterloesungsfelder und Loesungsfelder formatieren
        String musterloesungString = formatMap(filteredMusterloesungFelder);
        
        // In der Vorschau verwenden wir Platzhalter für Lösungsfelder
        Map<String, String> exampleSolutionFields = new HashMap<>();
        for (String key : actualFieldNames) {
            exampleSolutionFields.put(key, "[Nutzerantwort für Feld '" + key + "']");
        }
        String loesungString = formatMap(exampleSolutionFields);
        
        // Variablen für Template vorbereiten
        Map<String, Object> variables = new HashMap<>();
        variables.put("aufgabe", combinedMarkdown);
        variables.put("musterloesung", musterloesungString);
        variables.put("antwort", loesungString);
        variables.put("format", formatPlaceholder);
        
        // Bewertungshinweise hinzufügen, falls vorhanden
        if (teilaufgabeDto.getMusterloesungBewertungshinweise() != null && 
            !teilaufgabeDto.getMusterloesungBewertungshinweise().isBlank()) {
            variables.put("bewertungshinweise", teilaufgabeDto.getMusterloesungBewertungshinweise());
        } else {
            variables.put("bewertungshinweise", "");
        }
        
        // Template mit Variablen füllen und String zurückgeben
        return template.render(variables);
    }

    /**
     * Extrahiert die tatsächlich in der Aufgabenstellung vorhandenen Eingabefelder.
     * 
     * @param markdown Der Markdown-Text der Aufgabenstellung
     * @return Eine Liste der tatsächlich vorhandenen Input-Felder
     */
    private List<InputFieldDto> extractActualFields(String markdown) {
        // Wir verwenden die Vorschau-Methode, da wir keine spezielle Verarbeitung benötigen
        // und 0L als Kurseinheit-ID, da wir keine Bilder verarbeiten müssen
        return aufgabenMarkdownService.extractInputFields(markdown);
    }

    /**
     * Formatiert eine Map zu einem String für den Prompt.
     * 
     * @param map Die zu formatierende Map
     * @return Ein formatierter String
     */
    private String formatMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey())
              .append(": ")
              .append(entry.getValue())
              .append("\n");
        }
        return sb.toString();
    }
}