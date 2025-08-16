package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO für eine einzelne Nachricht in einem Chat.
 * Kann vom Studenten oder vom System stammen und kann Bezüge zu mehreren Kursmaterialien haben.
 */
@Getter
@Setter
public class ChatNachrichtDTO extends BaseDTO {
    
    /**
     * Der Zeitpunkt der Nachricht.
     */
    private LocalDateTime zeitpunkt;
    
    /**
     * Der Inhalt der Nachricht.
     */
    private String inhalt;
    
    /**
     * Der gerenderte HTML-Inhalt der Nachricht (für Markdown-Rendering).
     * Wird nur für System-Nachrichten verwendet.
     */
    private String inhaltHtml;
    
    /**
     * Gibt an, ob die Nachricht vom System oder vom Studenten stammt.
     */
    private Boolean istSystemNachricht;
    
    /**
     * Die ID des zugehörigen Chats.
     */
    private Long chatId;

    /**
     * Die referenzierten Kursmaterialien mit Seitenangaben.
     */
    private List<ChatNachrichtReferenzDTO> referenzen = new ArrayList<>();
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity
     */
    @Override
    public String getEntityTypeName() {
        return "ChatNachricht";
    }

    /**
     * Gibt den Anzeigenamen des zugehörigen Entity zurück.
     * 
     * @return Anzeigename der Chatnachricht
     */
    @Override
    public String getEntityDisplayName() {
        return inhalt != null && inhalt.length() > 30 ? 
                inhalt.substring(0, 27) + "..." : 
                (inhalt != null ? inhalt : "Chatnachricht");
    }
}