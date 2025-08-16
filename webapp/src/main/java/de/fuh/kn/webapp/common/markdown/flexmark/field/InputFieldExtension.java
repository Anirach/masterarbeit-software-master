package de.fuh.kn.webapp.common.markdown.flexmark.field;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Extension für Flexmark, die die Verarbeitung von Eingabefeldern ermöglicht.
 * Registriert den DelimiterProcessor und NodeRenderer.
 */
public class InputFieldExtension implements HtmlRenderer.HtmlRendererExtension, Parser.ParserExtension {

    /**
     * Privater Konstruktor, um die Factory-Methode zu nutzen.
     */
    private InputFieldExtension() {}

    /**
     * Factory-Methode zum Erstellen einer neuen Extension-Instanz.
     *
     * @return Eine neue InputFieldExtension-Instanz
     */
    public static InputFieldExtension create() {
        return new InputFieldExtension();
    }

    /**
     * Setzt Renderer-Optionen.
     *
     * @param options Die zu konfigurierenden Optionen
     */
    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
        // Keine spezifischen Optionen notwendig
    }

    /**
     * Setzt Parser-Optionen.
     *
     * @param options Die zu konfigurierenden Optionen
     */
    @Override
    public void parserOptions(MutableDataHolder options) {
        // Keine spezifischen Optionen notwendig
    }

    /**
     * Erweitert den Parser um den DelimiterProcessor.
     *
     * @param parserBuilder Der Parser-Builder
     */
    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new InputFieldDelimiterProcessor());
    }

    /**
     * Erweitert den HTML-Renderer um den NodeRenderer.
     *
     * @param htmlRendererBuilder Der HTML-Renderer-Builder
     * @param rendererType Der Renderer-Typ
     */
    @Override
    public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
        htmlRendererBuilder.nodeRendererFactory(new InputFieldNodeRenderer.Factory());
    }
}