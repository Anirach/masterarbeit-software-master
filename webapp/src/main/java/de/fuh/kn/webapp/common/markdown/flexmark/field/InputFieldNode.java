package de.fuh.kn.webapp.common.markdown.flexmark.field;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * Repräsentiert ein Eingabefeld im Markdown-Dokument.
 * Ein Eingabefeld kann verschiedene Typen haben und wird durch geschweifte Klammern im Markdown markiert.
 */
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class InputFieldNode extends Node {

    /**
     * Der Typ des Eingabefelds (text, num, tex).
     */
    private String fieldType;
    
    /**
     * Die Größe des Eingabefelds in Zeichen.
     */
    private int fieldSize;
    
    /**
     * Der Index des Felds innerhalb des Dokuments.
     */
    private int fieldIndex;
    
    /**
     * Der Name des Felds, falls angegeben.
     */
    private String fieldName;
    
    /**
     * Gibt an, ob es sich um ein mehrzeiliges Eingabefeld handelt.
     */
    private boolean isArea;
    
    /**
     * Die öffnende Markierung ({).
     */
    private final BasedSequence openingMarker;
    
    /**
     * Die schließende Markierung (}).
     */
    private final BasedSequence closingMarker;
    
    /**
     * Der Inhalt des Feldes zwischen den Markierungen.
     */
    private final BasedSequence fieldContent;

    /**
     * Konstruktor für die Node mit Basissequenzen.
     *
     * @param chars Die gesamte Sequenz des Knotens
     * @param openingMarker Die öffnende Markierung
     * @param closingMarker Die schließende Markierung
     * @param fieldContent Der Inhalt des Feldes
     */
    public InputFieldNode(BasedSequence chars, BasedSequence openingMarker, BasedSequence closingMarker, BasedSequence fieldContent) {
        super(chars);
        this.openingMarker = openingMarker;
        this.closingMarker = closingMarker;
        this.fieldContent = fieldContent;
    }

    /**
     * Gibt die Segmente des Knotens zurück.
     *
     * @return Ein Array mit den Segmenten des Knotens
     */
    @Override
    public @NotNull BasedSequence[] getSegments() {
        return new BasedSequence[] { openingMarker, fieldContent, closingMarker };
    }
}
