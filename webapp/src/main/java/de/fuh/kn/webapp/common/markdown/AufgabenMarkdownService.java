package de.fuh.kn.webapp.common.markdown;

import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderer;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Service für die Verarbeitung von Markdown in Aufgaben.
 * Bietet Methoden zum Rendern von Markdown und Extrahieren von Input-Feldern.
 */
@Service
public class AufgabenMarkdownService {

    private final MarkdownRenderer markdownRenderer;


    /**
     * Konstruktor mit Dependency Injection für den MarkdownRenderer.
     *
     * @param markdownRenderer der zu verwendende MarkdownRenderer
     */
    @Autowired
    public AufgabenMarkdownService(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Rendert Markdown-Text für die Anzeige in der Anwendung.
     * Input-Felder werden als Eingabefelder dargestellt.
     * Bilder werden für die angegebene Kurseinheit transformiert.
     * Enthält Musterlösungsfelder für internen Gebrauch.
     *
     * @param markdown Der zu rendernde Markdown-Text
     * @param kurseinheitId Die ID der Kurseinheit für die Bildverarbeitung
     * @return Das MarkdownRenderResult mit HTML und Input-Feldern
     */
    public MarkdownRenderResult renderMarkdownForInput(String markdown, Long kurseinheitId) {
        return markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, kurseinheitId);
    }

    /**
     * Rendert Markdown-Text für die Vorschau.
     * Input-Felder werden als disabled Eingabefelder dargestellt.
     * Bilder werden für die angegebene Kurseinheit transformiert.
     *
     * @param markdown Der zu rendernde Markdown-Text
     * @param kurseinheitId Die ID der Kurseinheit für die Bildverarbeitung
     * @return Das MarkdownRenderResult mit HTML und Input-Feldern
     */
    public MarkdownRenderResult renderMarkdownForPreview(String markdown, Long kurseinheitId) {
        return markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, kurseinheitId);
    }

    /**
     * Rendert Markdown-Text für die Anzeige in der Studentenansicht.
     * Input-Felder werden als Eingabefelder dargestellt.
     * Musterlösungsfelder werden nicht gerendert.
     * Bilder werden für die angegebene Kurseinheit transformiert.
     *
     * @param markdown Der zu rendernde Markdown-Text
     * @param kurseinheitId Die ID der Kurseinheit für die Bildverarbeitung
     * @return Das MarkdownRenderResult mit HTML und Input-Feldern
     */
    public MarkdownRenderResult renderMarkdownForStudentInput(String markdown, Long kurseinheitId) {
        return markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.STUDENT_INPUT, kurseinheitId);
    }

    /**
     * Rendert Markdown-Text für die Anzeige der Lösungsansicht.
     * Input-Felder werden als disabled Eingabefelder dargestellt.
     * Musterlösungsfelder werden daneben angezeigt.
     * Bilder werden für die angegebene Kurseinheit transformiert.
     *
     * @param markdown Der zu rendernde Markdown-Text
     * @param kurseinheitId Die ID der Kurseinheit für die Bildverarbeitung
     * @return Das MarkdownRenderResult mit HTML und Input-Feldern
     */
    public MarkdownRenderResult renderMarkdownForSolution(String markdown, Long kurseinheitId) {
        return markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.SOLUTION, kurseinheitId);
    }

    /**
     * Extrahiert die Namen der Input-Felder aus dem Markdown-Text.
     * Verwendet standardmäßig 0 als kurseinheitId, da keine Bilder benötigt werden.
     *
     * @param markdown Der Markdown-Text
     * @return Eine Liste der Feldnamen
     */
    public List<InputFieldDto> extractInputFields(String markdown) {
        MarkdownRenderResult result = renderMarkdownForInput(markdown, 0L);
        return result.getInputFields();
    }

}