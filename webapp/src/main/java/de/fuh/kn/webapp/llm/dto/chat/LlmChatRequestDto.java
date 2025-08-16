package de.fuh.kn.webapp.llm.dto.chat;

import de.fuh.kn.webapp.aufgabenverwaltung.dto.AufgabeDto;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.LoesungsVersuchDTO;
import de.fuh.kn.webapp.aufgabenverwaltung.dto.TeilaufgabeDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

/**
 * DTO für Chat-Anfragen an den LLM-Service.
 * Enthält alle notwendigen Kontext-Informationen für die Chat-Generierung.
 */
@Getter
@Setter
public class LlmChatRequestDto {
    
    /**
     * Die aktuelle Benutzernachricht, auf die geantwortet werden soll.
     */
    private String userMessage;
    
    /**
     * Die ID des Kurses für die Filterung der Kursmaterialien.
     */
    private Long kursId;
    
    /**
     * Die Aufgabe als Kontext für die Chat-Generierung.
     */
    private AufgabeDto aufgabe;
    
    /**
     * Die Teilaufgabe als spezifischer Kontext.
     */
    private TeilaufgabeDto teilaufgabe;
    
    /**
     * Der neueste Lösungsversuch des Studenten für zusätzlichen Kontext.
     */
    private Optional<LoesungsVersuchDTO> loesungsversuch;
    
    /**
     * Die bisherige Chat-Historie für Kontext.
     */
    private List<ChatMessage> chatHistory;

    /**
     * Erklär-Modus
     */
    private boolean explanationMode;
    
    /**
     * Repräsentiert eine einzelne Nachricht in der Chat-Historie.
     */
    @Getter
    @Setter
    public static class ChatMessage {
        private String content;
        private boolean systemMessage;
        
        public ChatMessage(String content, boolean systemMessage) {
            this.content = content;
            this.systemMessage = systemMessage;
        }
    }
}