package de.fuh.kn.webapp.common.markdown;

import de.fuh.kn.webapp.common.markdown.flexmark.InputFieldDto;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderResult;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderer;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den AufgabenMarkdownService.
 */
@ExtendWith(MockitoExtension.class)
class AufgabenMarkdownServiceTest {

    @Mock
    private MarkdownRenderer markdownRenderer;

    @InjectMocks
    private AufgabenMarkdownService aufgabenMarkdownService;

    private List<InputFieldDto> inputFields;

    @BeforeEach
    void setUp() {
        // Testdaten für Input-Felder vorbereiten
        inputFields = new ArrayList<>();
        inputFields.add(InputFieldDto.builder().fieldName("name").fieldType("text").fieldIndex(0).build());
        inputFields.add(InputFieldDto.builder().fieldName("alter").fieldType("num").fieldIndex(1).build());
        inputFields.add(InputFieldDto.builder().fieldName("beschreibung").isMultiline(true).fieldIndex(2).build());
    }

    @Test
    @DisplayName("renderMarkdownForInput sollte richtig delegieren und Ergebnis zurückgeben")
    void testRenderMarkdownForInput() {
        // Arrange
        String markdown = "# Test\n\nEingabe: {name}";
        Long kurseinheitId = 123L;
        
        // Setup mock response
        MarkdownRenderResult inputResult = MarkdownRenderResult.builder()
                .html("<h1>Test</h1><p>Input</p>")
                .inputFields(inputFields)
                .build();
        when(markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, kurseinheitId))
                .thenReturn(inputResult);

        // Act
        MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForInput(markdown, kurseinheitId);

        // Assert
        assertNotNull(result);
        assertEquals("<h1>Test</h1><p>Input</p>", result.getHtml());
        assertEquals(3, result.getInputFields().size());
        assertEquals("name", result.getInputFields().get(0).getFieldName());

        // Verify
        verify(markdownRenderer, times(1)).renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, kurseinheitId);
    }

    @Test
    @DisplayName("renderMarkdownForPreview sollte richtig delegieren und Ergebnis zurückgeben")
    void testRenderMarkdownForPreview() {
        // Arrange
        String markdown = "# Test\n\nVorschau: {name}";
        Long kurseinheitId = 123L;
        
        // Setup mock response
        MarkdownRenderResult previewResult = MarkdownRenderResult.builder()
                .html("<h1>Test</h1><p>Preview</p>")
                .inputFields(inputFields)
                .build();
        when(markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, kurseinheitId))
                .thenReturn(previewResult);

        // Act
        MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForPreview(markdown, kurseinheitId);

        // Assert
        assertNotNull(result);
        assertEquals("<h1>Test</h1><p>Preview</p>", result.getHtml());
        assertEquals(3, result.getInputFields().size());
        assertEquals("name", result.getInputFields().get(0).getFieldName());

        // Verify
        verify(markdownRenderer, times(1)).renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.PREVIEW, kurseinheitId);
    }

    @Test
    @DisplayName("extractInputFields sollte Input-Felder aus dem Markdown extrahieren")
    void testExtractInputFields() {
        // Arrange
        String markdown = "# Test\n\nFelder: {{name}}, {{alter}}, {{{beschreibung}}}";
        
        // Setup mock response
        MarkdownRenderResult inputResult = MarkdownRenderResult.builder()
                .html("<h1>Test</h1><p>Input</p>")
                .inputFields(inputFields)
                .build();
        when(markdownRenderer.renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, 0L))
                .thenReturn(inputResult);

        // Act
        List<InputFieldDto> fields = aufgabenMarkdownService.extractInputFields(markdown);

        // Assert
        assertEquals(inputFields, fields);

        // Verify - Hier prüfen wir, dass renderMarkdownForInput mit kurseinheitId 0 aufgerufen wird
        verify(markdownRenderer, times(1)).renderMarkdown(markdown, InputFieldNodeRenderer.RenderMode.INPUT, 0L);
    }

    @Test
    @DisplayName("Verschiedene Markdown-Syntax sollte korrekt verarbeitet werden")
    void testDifferentMarkdownSyntax() {
        // Arrange
        String complexMarkdown = "# Komplexer Test\n\n## Mit verschiedenen Elementen\n\n" +
                "- Liste Element 1\n- Liste Element 2\n\n" +
                "Ein Textfeld: {{{textfeld}}}\n\n" +
                "Eine Tabelle:\n\n" +
                "| Spalte 1 | Spalte 2 |\n| --- | --- |\n| Zelle 1 | Zelle 2 |\n\n" +
                "Ein Bild: ![Testbild](bild.png)";

        MarkdownRenderResult complexResult = MarkdownRenderResult.builder()
                .html("<h1>Komplexer Test</h1><p>Mit komplexen Elementen</p>")
                .inputFields(Collections.singletonList(InputFieldDto.builder().fieldName("textfeld").isMultiline(true).build()))
                .build();

        when(markdownRenderer.renderMarkdown(complexMarkdown, InputFieldNodeRenderer.RenderMode.INPUT, 1L)).thenReturn(complexResult);

        // Act
        MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForInput(complexMarkdown, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("<h1>Komplexer Test</h1><p>Mit komplexen Elementen</p>", result.getHtml());
        assertEquals(1, result.getInputFields().size());
        assertEquals("textfeld", result.getInputFields().get(0).getFieldName());

        // Verify
        verify(markdownRenderer, times(1)).renderMarkdown(complexMarkdown, InputFieldNodeRenderer.RenderMode.INPUT, 1L);
    }

    @Test
    @DisplayName("Leerer Markdown-Text sollte korrekt verarbeitet werden")
    void testEmptyMarkdown() {
        // Arrange
        String emptyMarkdown = "";
        MarkdownRenderResult emptyResult = MarkdownRenderResult.builder()
                .html("")
                .inputFields(new ArrayList<>())
                .build();

        when(markdownRenderer.renderMarkdown(emptyMarkdown, InputFieldNodeRenderer.RenderMode.INPUT, 1L)).thenReturn(emptyResult);

        // Act
        MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForInput(emptyMarkdown, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getHtml());
        assertEquals(0, result.getInputFields().size());

        // Verify
        verify(markdownRenderer, times(1)).renderMarkdown(emptyMarkdown, InputFieldNodeRenderer.RenderMode.INPUT, 1L);
    }

    @Test
    @DisplayName("Markdown mit speziellen Zeichen sollte korrekt verarbeitet werden")
    void testSpecialCharactersInMarkdown() {
        // Arrange
        String specialMarkdown = "# Test mit *speziellen* Zeichen\n\n" +
                "> Ein Zitat\n\n" +
                "Ein Code-Block:\n```java\nSystem.out.println(\"Hello World\");\n```\n\n" +
                "Ein Inputfeld: {spezial}";

        MarkdownRenderResult specialResult = MarkdownRenderResult.builder()
                .html("<h1>Test mit <em>speziellen</em> Zeichen</h1><blockquote><p>Ein Zitat</p></blockquote>")
                .inputFields(Collections.singletonList(InputFieldDto.builder().fieldName("spezial").build()))
                .build();

        when(markdownRenderer.renderMarkdown(specialMarkdown, InputFieldNodeRenderer.RenderMode.PREVIEW, 1L)).thenReturn(specialResult);

        // Act
        MarkdownRenderResult result = aufgabenMarkdownService.renderMarkdownForPreview(specialMarkdown, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("<h1>Test mit <em>speziellen</em> Zeichen</h1><blockquote><p>Ein Zitat</p></blockquote>", result.getHtml());
        assertEquals(1, result.getInputFields().size());
        assertEquals("spezial", result.getInputFields().get(0).getFieldName());

        // Verify
        verify(markdownRenderer, times(1)).renderMarkdown(specialMarkdown, InputFieldNodeRenderer.RenderMode.PREVIEW, 1L);
    }
}