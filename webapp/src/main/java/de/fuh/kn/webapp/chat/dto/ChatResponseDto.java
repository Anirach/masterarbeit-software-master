package de.fuh.kn.webapp.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO für ausgehende Chat-Antworten über die REST-API.
 * Enthält die Informationen über einen Chat und seine Nachrichten
 * für die Anzeige in der UI.
 */
@Getter
@Setter
public class ChatResponseDto {
    
    /**
     * ID des Chats.
     */
    private Long id;
    
    /**
     * Zeitpunkt des Chat-Beginns.
     */
    private LocalDateTime zeitpunkt;
    
    /**
     * Titel der zugehörigen Aufgabe für die Anzeige.
     */
    private String aufgabenTitel;
    
    /**
     * Die letzte Nachricht im Chat für die Übersichtsdarstellung.
     */
    private String letzteNachricht;
    
    /**
     * Die vom System generierte Antwort auf die letzte Anfrage.
     */
    private String systemAntwort;
    
    /**
     * Liste aller Nachrichten im Chat.
     * Wird nur bei detaillierter Anzeige eines Chats befüllt.
     */
    private List<ChatNachrichtDTO> nachrichten = new ArrayList<>();
    
    /**
     * Gibt an, ob der Chat noch aktiv ist oder abgeschlossen wurde.
     */
    private boolean aktiv = true;
}