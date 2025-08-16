package de.fuh.kn.webapp.llm.dto.pdfimport;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

/**
 * DTO für Anfragen zur LLM-basierten Extraktion von Aufgaben aus zwei PDF-Dateien (Aufgaben und Lösungen).
 * Enthält alle notwendigen Informationen für die Verarbeitung beider PDF-Dateien durch das LLM.
 */
@Getter
@Setter
public class LlmPdfPairImportRequestDto {
    
    /**
     * Die Aufgaben-PDF-Datei als Resource für die LLM-Verarbeitung.
     */
    private Resource assignmentResource;
    
    /**
     * Der Content-Type der Aufgaben-PDF-Datei.
     */
    private String assignmentContentType;
    
    /**
     * Die Lösungs-PDF-Datei als Resource für die LLM-Verarbeitung.
     */
    private Resource solutionResource;
    
    /**
     * Der Content-Type der Lösungs-PDF-Datei.
     */
    private String solutionContentType;
    
    /**
     * Optionale zusätzliche Anweisungen für die Extraktion.
     */
    private String additionalInstructions;
    
    /**
     * Erstellt eine neue Anfrage für die PDF-Paar-Verarbeitung.
     *
     * @param assignmentResource Die Aufgaben-PDF-Datei als Resource
     * @param assignmentContentType Der Content-Type der Aufgaben-Datei
     * @param solutionResource Die Lösungs-PDF-Datei als Resource
     * @param solutionContentType Der Content-Type der Lösungs-Datei
     */
    public LlmPdfPairImportRequestDto(Resource assignmentResource, String assignmentContentType,
                                     Resource solutionResource, String solutionContentType) {
        this.assignmentResource = assignmentResource;
        this.assignmentContentType = assignmentContentType;
        this.solutionResource = solutionResource;
        this.solutionContentType = solutionContentType;
    }
    
    /**
     * Standard-Konstruktor für Framework-Kompatibilität.
     */
    public LlmPdfPairImportRequestDto() {
    }
}