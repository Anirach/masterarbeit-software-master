package de.fuh.kn.webapp.llm.dto.chat;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO für Chat-Antworten vom LLM-Service.
 * Enthält die generierte Antwort sowie Metadaten zu Token-Nutzung und Referenzen.
 */
@Getter
@Setter
public class LlmChatResponseDto {
    
    /**
     * Der generierte Antworttext.
     */
    private String content;
    
    /**
     * Anzahl der verbrauchten Input-Tokens.
     */
    private Integer inputTokens;
    
    /**
     * Anzahl der generierten Output-Tokens.
     */
    private Integer outputTokens;
    
    /**
     * Das verwendete Modell.
     */
    private String model;
    
    /**
     * Die Kosten für diese Anfrage.
     */
    private BigDecimal cost;
    
    /**
     * Liste der referenzierten Dokumente aus dem RAG-System.
     */
    private List<DocumentReference> documentReferences;
    
    /**
     * Repräsentiert eine Referenz auf ein Kursmaterial-Dokument.
     */
    @Getter
    @Setter
    public static class DocumentReference {
        /**
         * ID des Kursmaterials.
         */
        private Long kursMaterialId;
        
        /**
         * Seitennummer innerhalb des Dokuments (optional).
         */
        private Integer pageNumber;
    }
}