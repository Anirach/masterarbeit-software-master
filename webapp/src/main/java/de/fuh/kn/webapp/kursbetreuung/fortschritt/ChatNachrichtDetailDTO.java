package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import de.fuh.kn.webapp.chat.dto.ChatNachrichtReferenzDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO für die Detailansicht einer Chat-Nachricht.
 * 
 * Enthält alle relevanten Informationen für die Kursbetreuung zur Einsicht
 * der Chat-Kommunikation zwischen Student und KI-System.
 */
@Data
@Builder
public class ChatNachrichtDetailDTO {
    
    /**
     * Basis-Informationen
     */
    private Long id;
    private LocalDateTime zeitpunkt;
    private String inhalt;
    
    /**
     * Gerendertes HTML aus Markdown-Inhalt für System-Nachrichten
     */
    private String inhaltHtml;
    
    /**
     * Nachrichtentyp
     */
    private boolean istSystemNachricht;
    
    /**
     * KI-Kosten für diese Nachricht
     */
    private BigDecimal kosten;
    
    /**
     * Referenzen zu Kursmaterialien
     */
    private List<ChatNachrichtReferenzDTO> referenzen;
}