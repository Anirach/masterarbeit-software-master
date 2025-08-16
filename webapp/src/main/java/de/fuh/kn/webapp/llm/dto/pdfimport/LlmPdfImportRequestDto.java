package de.fuh.kn.webapp.llm.dto.pdfimport;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

/**
 * DTO für Anfragen zur LLM-basierten Extraktion von Aufgaben aus einer einzelnen PDF-Datei.
 * Enthält alle notwendigen Informationen für die Verarbeitung der PDF-Datei durch das LLM.
 */
@Getter
@Setter
public class LlmPdfImportRequestDto {
    
    /**
     * Die PDF-Datei als Resource für die LLM-Verarbeitung.
     */
    private Resource resource;
    
    /**
     * Der Content-Type der PDF-Datei.
     */
    private String contentType;
    
    /**
     * Optionale zusätzliche Anweisungen für die Extraktion.
     */
    private String additionalInstructions;
    
    /**
     * Erstellt eine neue Anfrage für die PDF-Verarbeitung.
     *
     * @param resource Die PDF-Datei als Resource
     * @param contentType Der Content-Type der Datei
     */
    public LlmPdfImportRequestDto(Resource resource, String contentType) {
        this.resource = resource;
        this.contentType = contentType;
    }
    
    /**
     * Standard-Konstruktor für Framework-Kompatibilität.
     */
    public LlmPdfImportRequestDto() {
    }
}