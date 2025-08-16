package de.fuh.kn.webapp.llm.dto.pdfimport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO für die Antwort des LLM bei der Extraktion von Aufgaben aus PDF-Dateien.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmPdfImportResponseDto {
    
    /**
     * Liste der extrahierten Aufgaben aus der PDF-Datei.
     */
    private List<AufgabeDto> aufgaben = new ArrayList<>();
    
    /**
     * Anzahl der Eingabe-Tokens für die Anfrage an das LLM.
     */
    private Integer inputToken;
    
    /**
     * Anzahl der Ausgabe-Tokens für die Antwort des LLM.
     */
    private Integer outputToken;
    
    /**
     * Name des verwendeten LLM-Modells.
     */
    private String model;
    
    /**
     * Kosten für die API-Anfrage in USD.
     */
    private BigDecimal cost;
}