package de.fuh.kn.webapp.common.markdown.flexmark.field;

import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.core.delimiter.Delimiter;
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor;
import com.vladsch.flexmark.parser.delimiter.DelimiterRun;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verarbeitet Strings im Format {text:20}, {num:5}, {tex}, {{{text}}} zu Input-Feldern
 */
public class InputFieldDelimiterProcessor implements DelimiterProcessor {

    /**
     * Das Pattern für die Erkennung von Feldern.
     * Format: [name]:[typ]:[größe] - alle Teile sind optional.
     */
    // Pattern für die drei möglichen Feldtypen
    private static final Pattern SIMPLE_FIELD_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)$");
    private static final Pattern TYPED_FIELD_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+):(?:(text|num|tex)(?::(\\d+))?)?$");

    /**
     * Fortlaufender Index für Felder ohne expliziten Namen.
     */
    private int fieldIndex = 1;

    /**
     * Prüft, ob sich die aktuelle Position innerhalb von Math-Delimitern ($ $) befindet.
     * 
     * @param opener Der Öffnungs-Delimiter mit Zugriff auf die Eingabesequenz
     * @return true, wenn sich die Position innerhalb von Math-Delimitern befindet
     */
    private boolean isInsideMathDelimiters(Delimiter opener) {
        BasedSequence input = opener.getInput();
        int position = opener.getStartIndex();
        
        if (input == null || position <= 0) {
            return false;
        }
        
        // Zähle die Anzahl der '$' Zeichen vor der aktuellen Position in der aktuellen Zeile
        int dollarCount = 0;
        
        // Gehe rückwärts durch den Text bis zum Zeilenanfang oder Dokumentanfang
        for (int i = position - 1; i >= 0; i--) {
            char c = input.charAt(i);
            
            if (c == '$') {
                // Prüfe, ob das $ escaped ist (d.h. ein \ direkt davor steht)
                boolean isEscaped = false;
                if (i > 0 && input.charAt(i - 1) == '\\') {
                    // Zähle die Anzahl der aufeinanderfolgenden Backslashes vor dem $
                    int backslashCount = 0;
                    for (int j = i - 1; j >= 0 && input.charAt(j) == '\\'; j--) {
                        backslashCount++;
                    }
                    // Das $ ist nur escaped, wenn eine ungerade Anzahl von Backslashes davor steht
                    isEscaped = backslashCount % 2 == 1;
                }
                
                if (!isEscaped) {
                    dollarCount++;
                }
            }
            
            // Stoppe bei Zeilenwechsel, da Math-Delimiters normalerweise inline sind
            if (c == '\n' || c == '\r') {
                break;
            }
        }
        
        // Ungerade Anzahl von $ bedeutet, dass wir uns innerhalb eines Math-Ausdrucks befinden
        return dollarCount % 2 == 1;
    }

    /**
     * Gibt das Öffnungszeichen für das Delimiter zurück.
     *
     * @return Das Öffnungszeichen ('{')
     */
    @Override
    public char getOpeningCharacter() {
        return '{';
    }

    /**
     * Gibt das Schließzeichen für das Delimiter zurück.
     *
     * @return Das Schließzeichen ('}')
     */
    @Override
    public char getClosingCharacter() {
        return '}';
    }

    /**
     * Gibt die minimale Länge des Delimiters zurück.
     *
     * @return Die minimale Länge (1)
     */
    @Override
    public int getMinLength() {
        return 1;
    }

    /**
     * Prüft, ob das Zeichen ein Öffnungs-Delimiter sein kann.
     *
     * @return true, da '{' immer als Tag-Anfang erkannt werden soll.
     */
    @Override
    public boolean canBeOpener(String before, String after, boolean leftFlanking, boolean rightFlanking, 
                              boolean beforeIsPunctuation, boolean afterIsPunctuation, 
                              boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
        // { soll immer als Tag-Anfang erkannt werden
        return true;
    }

    /**
     * Prüft, ob das Zeichen ein Schließ-Delimiter sein kann.
     *
     * @return true, da '}' immer als Tag-Ende erkannt werden soll.
     */
    @Override
    public boolean canBeCloser(String before, String after, boolean leftFlanking, boolean rightFlanking, 
                              boolean beforeIsPunctuation, boolean afterIsPunctuation, 
                              boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
        // } soll immer als Tag-Ende erkannt werden
        return true;
    }

    /**
     * Bestimmt, ob nicht passende Schließ-Delimiter übersprungen werden sollen.
     *
     * @return false, damit alle geschweiften Klammern verarbeitet werden.
     */
    @Override
    public boolean skipNonOpenerCloser() {
        // Keine geschweiften Klammern überspringen, immer verarbeiten
        return false;
    }

    /**
     * Bestimmt, wie viele Delimiter verwendet werden sollen.
     *
     * @param opener Der Öffnungs-Delimiter
     * @param closer Der Schließ-Delimiter
     * @return Die Anzahl der zu verwendenden Delimiter
     */
    @Override
    public int getDelimiterUse(DelimiterRun opener, DelimiterRun closer) {
        // Prüfe, ob wir uns innerhalb von Math-Delimitern befinden
        // Wenn ja, verwende keine Delimiter (0), damit sie als normale Zeichen behandelt werden
        if (opener instanceof Delimiter && isInsideMathDelimiters((Delimiter) opener)) {
            return 0;
        }
        
        // Immer alle Delimiter benutzen
        return Math.min(opener.length(), closer.length());
    }

    /**
     * Verarbeitet die Delimiter und erstellt einen InputFieldNode.
     *
     * @param opener Der Öffnungs-Delimiter
     * @param closer Der Schließ-Delimiter
     * @param delimitersUsed Die Anzahl der verwendeten Delimiter
     */
    @Override
    public void process(Delimiter opener, Delimiter closer, int delimitersUsed) {
        // Basissequenzen ermitteln
        BasedSequence openingMarker = opener.getTailChars(delimitersUsed);
        BasedSequence closingMarker = closer.getLeadChars(delimitersUsed);
        BasedSequence contentsSequence = opener.getNextNonDelimiterTextNode().getChars();

        // Prüfen ob es eine Area ist (3 geschweifte Klammern statt nur einer)
        boolean isTextArea = delimitersUsed >= 3;

        // Art des Feldes ermitteln
        String content = contentsSequence.toString().trim();
        String fieldName = null;
        String fieldType = "text"; // Default ist "text"
        int size = 30; // Default-Größe ist 30
        
        // Versuche die verschiedenen Muster zu matchen
        Matcher simpleMatcher = SIMPLE_FIELD_PATTERN.matcher(content);
        Matcher typedMatcher = TYPED_FIELD_PATTERN.matcher(content);

        if (simpleMatcher.matches()) {
            // Einfaches Feld: {feldname}
            fieldName = simpleMatcher.group(1);
        } else if (typedMatcher.matches()) {
            // Feld mit Typ: {feldname:typ:größe}
            fieldName = typedMatcher.group(1);
            String type = typedMatcher.group(2);
            if (type != null) {
                fieldType = type;
            }
            String sizeStr = typedMatcher.group(3);
            if (sizeStr != null) {
                size = Integer.parseInt(sizeStr);
            }
        } else {
            // Bei unbekanntem Format, versuche den gesamten Inhalt als Feldnamen zu verwenden
            // Falls es leer ist, verwende einen generischen Namen
            fieldName = content.isEmpty() ? "field" + fieldIndex : content;
        }

        // Node erstellen
        InputFieldNode node = new InputFieldNode(opener.getInput(), openingMarker, closingMarker, contentsSequence);
        node.setFieldType(fieldType);
        node.setFieldSize(size);
        node.setFieldIndex(fieldIndex++);
        node.setFieldName(fieldName);
        node.setArea(isTextArea);

        // neue Node in Dokument einfügen und alten Inhalt entfernen
        opener.getNode().insertBefore(node);

        opener.getNextNonDelimiterTextNode().unlink();
        opener.getNode().unlink();
        closer.getNode().unlink();
    }

    /**
     * Gibt einen Knoten für nicht übereinstimmende Delimiter zurück.
     *
     * @return null, da wir keine speziellen Knoten für nicht übereinstimmende Delimiter erstellen.
     */
    @Override
    public Node unmatchedDelimiterNode(InlineParser inlineParser, DelimiterRun delimiter) {
        return null;
    }
}