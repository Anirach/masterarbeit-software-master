package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO für einen Chat zwischen einem Studenten und dem System.
 * Enthält grundlegende Informationen über den Chat sowie eine Liste von Nachrichten.
 */
@Getter
@Setter
public class ChatDTO extends BaseDTO {
    
    /**
     * Der Zeitpunkt des Chat-Beginns.
     */
    private LocalDateTime zeitpunkt;
    
    /**
     * Die Anzahl der verwendeten LLM-Token für den Chat.
     */
    private Integer llmToken;
    
    /**
     * Die ID des zugehörigen Studenten.
     */
    private Long studentId;
    
    /**
     * Die ID der zugehörigen Teilaufgabe.
     */
    private Long teilaufgabeId;

    /**
     * Titel der Aufgabe (für Anzeigezwecke)
     */
    private String aufgabenTitel;

    /**
     * Reihenfolge der Teilaufgabe (für Anzeigezwecke)
     */
    private Integer teilaufgabeReihenfolge;
    
    /**
     * Die Nachrichten des Chats.
     */
    private List<ChatNachrichtDTO> nachrichten = new ArrayList<>();
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity
     */
    @Override
    public String getEntityTypeName() {
        return "Chat";
    }

    /**
     * Gibt den Anzeigenamen des zugehörigen Entity zurück.
     * 
     * @return Anzeigename des Chats
     */
    @Override
    public String getEntityDisplayName() {
        String displayName = "Chat zu Aufgabe: " + (aufgabenTitel != null ? aufgabenTitel : "Unbekannt");
        if (teilaufgabeReihenfolge != null) {
            displayName += " (Teilaufgabe " + teilaufgabeReihenfolge + ")";
        }
        return displayName;
    }
}