package de.fuh.kn.webapp.llm.dto.bewertung;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO für die Anfrage zur Bewertung einer Lösung.
 * Enthält die Aufgabenstellung, die Musterlösung und die eingereichte Lösung.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BewertungsRequestDto {

    /**
     * Die ID der Teilaufgabe.
     */
    private Long teilaufgabeId;
    
    /**
     * Die ID des Studenten, falls bekannt.
     * Wird für die Zuordnung von Kosten verwendet.
     */
    private Long studentId;
    
    /**
     * Die Aufgabenstellung der Hauptaufgabe im Markdown-Format.
     */
    private String aufgabenstellungAufgabe;
    
    /**
     * Die Aufgabenstellung der Teilaufgabe im Markdown-Format.
     */
    private String aufgabenstellungTeilaufgabe;
    
    /**
     * Bewertungshinweise zur Aufgabe.
     */
    private String bewertungshinweise;
    
    /**
     * Die Musterlösung für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die erwartete Lösung.
     */
    @Builder.Default
    private Map<String, String> musterloesungFelder = new HashMap<>();
    
    /**
     * Die eingereichte Lösung für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die eingereichte Lösung.
     */
    @Builder.Default
    private Map<String, String> loesungFelder = new HashMap<>();
}