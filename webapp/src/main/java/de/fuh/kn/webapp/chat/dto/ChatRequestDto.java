package de.fuh.kn.webapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO für eingehende Chat-Anfragen über die REST-API.
 * Enthält die nötigen Informationen, um einen Chat zu starten
 * oder eine Nachricht zu senden.
 */
@Getter
@Setter
public class ChatRequestDto {
    
    /**
     * ID des Chats für bestehende Chats.
     * Kann null sein, wenn ein neuer Chat gestartet wird.
     */
    private Long chatId;
    
    /**
     * ID der Aufgabe, für die ein Chat gestartet werden soll.
     * Wird nur bei der Erstellung eines neuen Chats verwendet.
     */
    private Long aufgabeId;
    
    /**
     * Nachrichteninhalt, der gesendet werden soll.
     */
    @NotBlank(message = "Nachricht darf nicht leer sein")
    private String nachricht;
}