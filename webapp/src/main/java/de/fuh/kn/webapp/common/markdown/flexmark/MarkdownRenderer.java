package de.fuh.kn.webapp.common.markdown.flexmark;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldExtension;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNode;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import de.fuh.kn.webapp.common.markdown.flexmark.image.KursMaterialImageExtension;
import de.fuh.kn.webapp.common.markdown.flexmark.image.KursMaterialImageNodeRenderer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Komponente zum Rendern von Markdown-Text mit speziellen Erweiterungen für Input-Felder.
 * Verwendet Flexmark für das Rendering und extrahiert die Input-Felder aus dem Markdown.
 */
@Component
public class MarkdownRenderer {

    /**
     * Rendert Markdown-Text und extrahiert die Input-Felder.
     * Bilder werden für die angegebene Kurseinheit transformiert.
     *
     * @param markdown    Der zu rendernde Markdown-Text
     * @param renderMode  Der Render-Modus für die Input-Felder (INPUT oder PREVIEW)
     * @param kurseinheitId Die ID der Kurseinheit (für die Bildverarbeitung)
     * @return Das MarkdownRenderResult mit dem gerenderten HTML und den extrahierten Input-Feldern
     */
    public MarkdownRenderResult renderMarkdown(String markdown, InputFieldNodeRenderer.RenderMode renderMode, Long kurseinheitId) {

        MutableDataSet options = new MutableDataSet();

        //Bootstrap-Klasse für Tabellen verwenden
        TablesExtension tablesExtension = TablesExtension.create();
        TablesExtension.CLASS_NAME.set(options, "table");

        // Options für den Parser und Renderer
        options.set(Parser.EXTENSIONS, Arrays.asList(
                tablesExtension,
                InputFieldExtension.create(),
                KursMaterialImageExtension.create()))
        .set(InputFieldNodeRenderer.RENDER_MODE, renderMode);
        
        // KurseinheitId als explizite DataKey setzen
        if (kurseinheitId != null && kurseinheitId > 0) {
            options.set(KursMaterialImageNodeRenderer.KURSEINHEIT_ID, kurseinheitId);
        }

        // Parser und Renderer erstellen
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).softBreak("<br/>\n").build();

        // Markdown parsen
        Node document = parser.parse(markdown);

        // Input-Felder extrahieren mit Node Visitor Pattern
        List<InputFieldDto> inputFields = extractInputFields(document, renderer);

        // HTML generieren
        String html = renderer.render(document);

        // Ergebnis zurückgeben
        return MarkdownRenderResult.builder()
                .html(html)
                .inputFields(inputFields)
                .build();
    }

    /**
     * Extrahiert die Input-Felder aus dem geparsten Markdown-Dokument mit dem Node Visitor Pattern.
     *
     * @param document Das geparste Markdown-Dokument
     * @param renderer Der HTML-Renderer
     * @return Die Liste der extrahierten Input-Felder
     */
    private List<InputFieldDto> extractInputFields(Node document, HtmlRenderer renderer) {
        List<InputFieldDto> result = new ArrayList<>();

        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(InputFieldNode.class, node -> {
                    InputFieldDto field = InputFieldDto.builder()
                            .fieldName(node.getFieldName())
                            .fieldType(node.getFieldType())
                            .fieldSize(node.getFieldSize())
                            .isMultiline(node.isArea())
                            .fieldIndex(node.getFieldIndex())
                            .html(renderer.render(node))
                            // Wir lassen die HTML-Generierung dem InputFieldNodeRenderer
                            .build();
                    
                    result.add(field);
                })
        );

        visitor.visit(document);
        return result;
    }
}