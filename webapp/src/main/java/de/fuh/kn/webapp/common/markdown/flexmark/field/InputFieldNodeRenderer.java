package de.fuh.kn.webapp.common.markdown.flexmark.field;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Renderer für InputFieldNodes, der verschiedene HTML-Darstellungen erzeugen kann.
 */
public class InputFieldNodeRenderer implements NodeRenderer {

    /**
     * Die Konfigurationsoptionen für den Renderer.
     */
    private final DataHolder options;

    /**
     * Der Render-Modus bestimmt, wie die Felder gerendert werden.
     */
    public static final DataKey<RenderMode> RENDER_MODE = new DataKey<>("INPUT_FIELD_RENDER_MODE", RenderMode.INPUT);

    /**
     * Enum für den Render-Modus.
     */
    public enum RenderMode {
        /**
         * Eingabemodus - Felder werden als interaktive Eingabefelder gerendert.
         * Keine Musterlösung wird angezeigt.
         */
        INPUT,

        /**
         * Vorschaumodus - Felder werden als nicht-editierbare Platzhalter gerendert.
         * Keine Musterlösung wird angezeigt.
         */
        PREVIEW,

        /**
         * Lösungsmodus - Felder werden als nicht-editierbare Felder mit Musterlösung gerendert.
         * Sowohl Eingabefelder als auch Musterlösungen werden angezeigt.
         */
        SOLUTION,

        /**
         * Studenten-Eingabemodus - Felder werden als interaktive Eingabefelder gerendert.
         * Wie INPUT, aber ohne Musterlösungsfelder.
         */
        STUDENT_INPUT
    }

    /**
     * Konstruktor mit Konfigurationsoptionen.
     *
     * @param options Die Konfigurationsoptionen
     */
    public InputFieldNodeRenderer(DataHolder options) {
        this.options = options;
    }

    /**
     * Gibt die NodeRenderingHandler für diesen Renderer zurück.
     *
     * @return Set mit NodeRenderingHandlern
     */
    @Override
    public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(
                new NodeRenderingHandler<>(
                        InputFieldNode.class,
                        (node, context, html) -> render(node, html)
                )
        );
    }

    /**
     * Rendert einen InputFieldNode in HTML.
     *
     * @param node Der zu rendernde Knoten
     * @param html Der HTML-Writer
     */
    public void render(@NotNull InputFieldNode node, @NotNull HtmlWriter html) {
        int fieldIndex = node.getFieldIndex();
        int fieldSize = node.getFieldSize();
        String fieldType = node.getFieldType();
        String fieldName = node.getFieldName();
        
        // Span-Element als Wrapper um jedes Eingabe-Feld
        // Klasse für Bewertungs-Farbe des Eingabefeldes (rot/gelb/grün)
        html
                .attr("th:class", "${#vars['field-" + fieldName + "-class']}")
                .attr("id", "field-" + fieldName + "-wrapper")
                .withAttr()
                .tagLineIndent("span", () -> {
                    if (node.isArea()) {
                        renderArea(html, fieldName);
                    } else {
                        renderInline(html, fieldSize, fieldName, fieldType);
                    }
                });
    }

    /**
     * Rendert ein mehrzeiliges Eingabefeld.
     *
     * @param html Der HTML-Writer
     * @param fieldName Der Name des Feldes
     */
    private void renderArea(@NotNull HtmlWriter html, String fieldName) {
        RenderMode renderMode = RENDER_MODE.get(options);

        // Im Lösungsmodus nur das Lösungsfeld anzeigen, nicht das Fragefeld
        if (renderMode.equals(RenderMode.SOLUTION)) {
            // Nur Musterlösung im Lösungsmodus als div anzeigen
            html.attr("class", "answer auto-resize solution-field multi-line-solution");
            html.attr("th:text", "${#vars['field-" + fieldName + "-solution']}");
            html.attr("style", "min-height: 4em; padding: 8px; white-space: pre-wrap;");
            html.withAttr();
            html.tag("div");
            html.tag("/div");
        } else {
            // Textarea für mehrzeilige Eingabe in allen anderen Modi
            html.attr("class", "question auto-resize");
            html.attr("id", fieldName);
            html.attr("name", fieldName);
            html.attr("rows", "4");
            html.attr("cols", "50");
            html.attr("th:text", "${#vars['field-" + fieldName + "']}");
            html.attr("data-cy", "input-field-" + fieldName);
            html.attr("autocomplete", "off");

            // In Vorschaumodus als disabled markieren
            if (renderMode.equals(RenderMode.PREVIEW)) {
                html.attr("disabled", "disabled");
            }

            html.withAttr();
            html.tag("textarea");

            // In Vorschaumodus Platzhaltertext anzeigen
            if (renderMode.equals(RenderMode.PREVIEW)) {
                html.text("(" + fieldName + ")");
            }

            html.tag("/textarea");
            html.line();
        }
    }

    /**
     * Rendert ein einzeiliges Eingabefeld.
     *
     * @param html Der HTML-Writer
     * @param fieldSize Die Größe des Feldes
     * @param fieldName Der Name des Feldes
     * @param fieldType Der Typ des Feldes
     */
    private void renderInline(@NotNull HtmlWriter html, int fieldSize, String fieldName, String fieldType) {
        RenderMode renderMode = RENDER_MODE.get(options);

        // Im Lösungsmodus nur das Lösungsfeld anzeigen, nicht das Fragefeld
        if (renderMode.equals(RenderMode.SOLUTION)) {
            // Nur Musterlösung im Lösungsmodus als div anzeigen
            html.attr("class", "answer solution-field inline-solution");
            double calculatedWidth = Math.max(3.0, fieldSize * 0.6);
            html.attr("style", "display: inline-block; min-width: " + calculatedWidth + "em; padding: 4px 8px;");
            html.attr("th:text", "${#vars['field-" + fieldName + "-solution']}");
            
            // Für TeX-Felder spezielle Klasse hinzufügen
            if (fieldType.equals("tex")) {
                html.attr("data-field-type", "tex");
                html.attr("data-field-name", fieldName);
            }
            
            html.withAttr();
            html.tag("div");
            html.tag("/div");
        } else {
            // Inline-Feld für Eingabe in allen anderen Modi
            double calculatedWidth = Math.max(3.0, fieldSize * 0.6);
            html.attr("style", "width: " + calculatedWidth + "em");
            html.attr("id", fieldName);
            html.attr("name", fieldName);
            html.attr("th:value", "${#vars['field-" + fieldName + "']}");
            html.attr("data-cy", "input-field-" + fieldName);
            html.attr("autocomplete", "off");

            if (fieldType.equals("num")) {
                html.attr("type", "text");
                html.attr("pattern", "-?[0-9]+([,.][0-9]+)?");
                html.attr("inputmode", "decimal");
                html.attr("class", "question number-field");
            } else {
                html.attr("type", "text");
                html.attr("class", "question");
            }

            //data-field-type=tex hinzufügen, damit das Feld von katex erfasst wird. Außer bei Preview, da soll katex nichts tun
            if (fieldType.equals("tex") && !renderMode.equals(RenderMode.PREVIEW)) {
                html.attr("data-field-type", "tex");
            }

            // In Vorschaumodus als disabled markieren
            if (renderMode.equals(RenderMode.PREVIEW)) {
                html.attr("disabled", "disabled");
                html.attr("value", "(" + fieldName + ")");
                html.attr("type", "text"); // Preview muss immer Text sein, damit der Value rein geht
            }

            html.withAttr();
            html.tagVoidLine("input");

            if (fieldType.equals("tex")) {
                html.attr("id", fieldName + "-tex");
                html.withAttr();
                html.tagVoidLine("span");
            }
        }
    }

    /**
     * Factory-Klasse für den InputFieldNodeRenderer.
     */
    public static class Factory implements NodeRendererFactory {
        /**
         * Erstellt einen neuen InputFieldNodeRenderer.
         *
         * @param options Die Konfigurationsoptionen
         * @return Ein neuer InputFieldNodeRenderer
         */
        @NotNull
        @Override
        public NodeRenderer apply(@NotNull DataHolder options) {
            return new InputFieldNodeRenderer(options);
        }
    }
}