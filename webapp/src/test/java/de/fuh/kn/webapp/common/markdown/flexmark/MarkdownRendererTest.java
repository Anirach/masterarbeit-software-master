package de.fuh.kn.webapp.common.markdown.flexmark;

import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
@SpringBootTest
class MarkdownRendererTest {

    private MarkdownRenderer markdownRenderer;
    private final Long TEST_KURSEINHEIT_ID = 123L;

    @BeforeEach
    void setUp() {
        markdownRenderer = new MarkdownRenderer();
    }

    /**
     * Testet das Rendern von einfachem Markdown ohne Input-Felder.
     */
    @Test
    void testRenderSimpleMarkdown() {
        String markdown = "# Überschrift\n\nEin einfacher Text.";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        assertNotNull(result.getHtml());
        assertTrue(result.getHtml().contains("<h1>Überschrift</h1>"));
        assertTrue(result.getHtml().contains("<p>Ein einfacher Text.</p>"));
        assertTrue(result.getInputFields().isEmpty());
    }

    /**
     * Testet das Rendern von Markdown mit einem einfachen Input-Feld.
     */
    @Test
    void testRenderMarkdownWithSimpleInputField() {
        String markdown = "# Aufgabe\n\nGeben Sie die Lösung ein: {antwort}";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        assertNotNull(result.getHtml());
        assertTrue(result.getHtml().contains("<h1>Aufgabe</h1>"));
        assertTrue(result.getHtml().contains("<p>Geben Sie die Lösung ein:"));
        assertTrue(result.getHtml().contains("<input"));
        List<InputFieldDto> fields = result.getInputFields();
        assertNotNull(fields);
        assertEquals(1, fields.size());
        
        InputFieldDto field = fields.get(0);
        assertEquals("antwort", field.getFieldName());
        assertEquals("text", field.getFieldType());
        assertFalse(field.isMultiline());
        assertEquals(1, field.getFieldIndex());
    }

    /**
     * Testet das Rendern von Markdown mit einem typisierten Input-Feld.
     */
    @Test
    void testRenderMarkdownWithTypedInputField() {
        String markdown = "# Aufgabe\n\nGeben Sie eine Zahl ein: {nummer:num:5}\n\nGeben Sie eine Formel ein: {formel:tex}";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        
        List<InputFieldDto> fields = result.getInputFields();
        assertEquals(2, fields.size());
        
        InputFieldDto field = fields.get(0);
        assertEquals("nummer", field.getFieldName());
        assertEquals("num", field.getFieldType());
        assertEquals(5, field.getFieldSize());
        assertFalse(field.isMultiline());

        InputFieldDto texField = fields.get(1);
        assertEquals("formel", texField.getFieldName());
        assertEquals("tex", texField.getFieldType());
        assertEquals(30, texField.getFieldSize());
        assertFalse(texField.isMultiline());
    }

    /**
     * Testet das Rendern von Markdown mit einem mehrzeiligen Input-Feld.
     */
    @Test
    void testRenderMarkdownWithMultilineInputField() {
        String markdown = "# Aufgabe\n\nSchreiben Sie einen kurzen Text:\n\n{{{textfeld}}}";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        
        List<InputFieldDto> fields = result.getInputFields();
        assertEquals(1, fields.size());
        
        InputFieldDto field = fields.get(0);
        assertEquals("textfeld", field.getFieldName());
        assertTrue(field.isMultiline());
    }

    /**
     * Testet das Rendern von Markdown mit mehreren Input-Feldern.
     */
    @Test
    void testRenderMarkdownWithMultipleInputFields() {
        String markdown = "# Aufgabe\n\n1. Name: {name}\n2. Alter: {alter:num}\n3. Beschreibung:\n\n{{{beschreibung}}}";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        
        List<InputFieldDto> fields = result.getInputFields();
        assertEquals(3, fields.size());
        
        // Erstes Feld prüfen
        InputFieldDto field1 = fields.get(0);
        assertEquals("name", field1.getFieldName());
        assertEquals("text", field1.getFieldType());
        assertFalse(field1.isMultiline());
        assertEquals(1, field1.getFieldIndex());
        
        // Zweites Feld prüfen
        InputFieldDto field2 = fields.get(1);
        assertEquals("alter", field2.getFieldName());
        assertEquals("num", field2.getFieldType());
        assertFalse(field2.isMultiline());
        assertEquals(2, field2.getFieldIndex());
        
        // Drittes Feld prüfen
        InputFieldDto field3 = fields.get(2);
        assertEquals("beschreibung", field3.getFieldName());
        assertTrue(field3.isMultiline());
        assertEquals(3, field3.getFieldIndex());
    }

    /**
     * Testet, dass das gerenderte HTML Eingabefelder enthält.
     */
    @Test
    void testRenderedHtmlContainsInputFields() {
        String markdown = "# Aufgabe\n\nGeben Sie die Lösung ein: {{antwort}}";
        
        // Im INPUT-Modus
        MarkdownRenderResult inputResult = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);
        String html = inputResult.getHtml();
        
        assertTrue(html.contains("input") || html.contains("textarea"));
        assertTrue(html.contains("name=\"antwort\""));
        
        // Im PREVIEW-Modus
        MarkdownRenderResult previewResult = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, TEST_KURSEINHEIT_ID);
        String previewHtml = previewResult.getHtml();
        
        assertTrue(previewHtml.contains("input") || previewHtml.contains("textarea"));
        assertTrue(previewHtml.contains("name=\"antwort\""));
        assertTrue(previewHtml.contains("disabled"));
    }
    
    /**
     * Testet das Rendern von Markdown mit Bildreferenzen.
     */
    @Test
    void testRenderMarkdownWithImages() {
        String markdown = "# Aufgabe mit Bild\n\n![Beispielbild](beispiel.png)";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);

        assertNotNull(result);
        assertNotNull(result.getHtml());
        
        // Prüft, ob die Bildurl transformiert wurde (sollte den kurseinheitId-Parameter enthalten)
        assertTrue(result.getHtml().contains("/material/kurseinheit/" + TEST_KURSEINHEIT_ID + "/bild?name=beispiel.png"));
        assertTrue(result.getHtml().contains("alt=\"Beispielbild\""));
    }

    @Test
    public void testRenderMarkdownWithLatex() {

        String mathMarkdown = "Gegeben sei die Funktion $ n^{\\sqrt{n}} = \\Omega(\\log n) $. " +
                "Berechnen Sie die Laufzeit.";
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(mathMarkdown, InputFieldNodeRenderer.RenderMode.INPUT, TEST_KURSEINHEIT_ID);


        assertEquals("<p>Gegeben sei die Funktion $ n^{\\sqrt{n}} = \\Omega(\\log n) $. Berechnen Sie die Laufzeit.</p>\n",  result.getHtml());

    }


    @Test
    public void testMarkdownRender(){
        String markdown = """
                **Warum konvergiert $n^{\\sqrt{n}}$ gegen 1?**
                
                Die Aussage, dass $n^{\\sqrt{n}}$ gegen 1 konvergiert, ist **falsch**.
                
                Wenn $n$ größer wird, wächst auch $\\sqrt{n}$, und damit wächst $n^{\\sqrt{n}}$ **extrem schnell**. Zum Vergleich:
                
                """;
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, TEST_KURSEINHEIT_ID);

        assertNotNull(result);


    }

    /**
     * Parametrized test to ensure proper handling of braces within math expressions vs. actual input fields
     */
    @ParameterizedTest
    @CsvSource({
        "'$\\sqrt{n}$', 0, 'Should not detect field in math without spaces'",
        "'$ \\sqrt{n} $', 0, 'Should not detect field in math with spaces'", 
        "'$f(x) = x^{2}$', 0, 'Should not detect field in math with exponent'",
        "'$\\frac{a}{b}$', 0, 'Should not detect field in fraction'",
        "'Enter value: {n}', 1, 'Should detect field in regular text'",
        "'$x = {a} + b$', 0, 'Should not detect field inside math'",
        "'Before math $\\sqrt{n}$ and after {field}', 1, 'Should only detect field outside math'",
        "'\\${escaped}', 1, 'Should detect field when dollar is escaped'",
        "'$\\${escaped}$', 0, 'Should not detect field when dollar inside math is escaped'"
    })
    void testMathDelimiterHandling(String markdown, int expectedFieldCount, String description) {
        MarkdownRenderResult result = markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, TEST_KURSEINHEIT_ID);
        
        assertEquals(expectedFieldCount, result.getInputFields().size(), description);
        
        // Additional validation for math expressions
        if (expectedFieldCount == 0 && markdown.contains("$")) {
            // Math expressions should preserve their braces in the output
            assertTrue(result.getHtml().contains("{") || !markdown.contains("{"), 
                      "Math expressions should preserve braces or not contain them");
        }
    }
}
