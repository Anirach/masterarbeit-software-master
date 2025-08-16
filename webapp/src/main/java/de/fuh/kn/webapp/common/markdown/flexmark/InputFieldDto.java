package de.fuh.kn.webapp.common.markdown.flexmark;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO für die Input-Felder im Markdown.
 * Repräsentiert ein einzelnes Eingabefeld, das aus dem Markdown extrahiert wurde.
 */
@Getter
@Setter
@Builder
public class InputFieldDto {
    
    /**
     * Der Name des Feldes, kann null sein.
     */
    private String fieldName;
    
    /**
     * Der Typ des Feldes (text, num, tex, etc.), kann null sein.
     */
    private String fieldType;
    
    /**
     * Die Größe des Feldes, kann null sein.
     */
    private Integer fieldSize;
    
    /**
     * Flag, ob es sich um ein mehrzeiliges Feld handelt.
     */
    private boolean isMultiline;
    
    /**
     * Der Index des Feldes im Markdown-Text.
     */
    private int fieldIndex;

    private String html;
}
