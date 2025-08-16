package de.fuh.kn.webapp.llm.dto.generator;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.kursverwaltung.dto.KurseinheitDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO für Anfragen zur LLM-basierten Aufgabengenerierung.
 * Enthält alle notwendigen Kontext-Informationen für die Generierung neuer Aufgaben.
 */
@Getter
@Setter
public class LlmGeneratorRequestDto {
    
    /**
     * Das Thema für die neue Aufgabe.
     */
    private String thema;
    
    /**
     * Die ID des Kurses für die Filterung der Kursmaterialien.
     */
    private Long kursId;
    
    /**
     * Die Kurseinheit als Kontext für die Aufgabengenerierung.
     */
    private KurseinheitDTO kurseinheit;
    
    /**
     * Liste von Beispielaufgaben zur Orientierung für das LLM.
     */
    private List<AufgabeDto> beispielaufgaben;
    
    /**
     * Standard-Konstruktor für Framework-Kompatibilität.
     */
    public LlmGeneratorRequestDto() {
    }
    
    /**
     * Konstruktor mit allen wesentlichen Parametern.
     *
     * @param thema Das Thema für die neue Aufgabe
     * @param kursId Die Kurs-ID für die Materialfilterung
     * @param kurseinheit Die Kurseinheit als Kontext
     * @param beispielaufgaben Liste der Beispielaufgaben
     */
    public LlmGeneratorRequestDto(String thema, Long kursId, KurseinheitDTO kurseinheit, List<AufgabeDto> beispielaufgaben) {
        this.thema = thema;
        this.kursId = kursId;
        this.kurseinheit = kurseinheit;
        this.beispielaufgaben = beispielaufgaben;
    }
}