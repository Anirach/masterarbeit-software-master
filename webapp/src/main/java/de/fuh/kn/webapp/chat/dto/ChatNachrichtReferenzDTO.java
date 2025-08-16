package de.fuh.kn.webapp.chat.dto;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO für eine Referenz auf ein Kursmaterial innerhalb einer ChatNachricht.
 */
@Getter
@Setter
public class ChatNachrichtReferenzDTO extends BaseDTO {
    
    /**
     * Die ID der zugehörigen ChatNachricht.
     */
    private Long chatNachrichtId;
    
    /**
     * Die ID des referenzierten Kursmaterials.
     */
    private Long kursMaterialId;
    
    /**
     * Der Name des referenzierten Kursmaterials (für Anzeigezwecke).
     */
    private String kursMaterialName;
    
    /**
     * Die Seitennummer im Dokument (optional).
     */
    private Integer seitennummer;
    
    /**
     * Gibt den Klassennamen der zugehörigen Entity zurück.
     *
     * @return Der Klassenname der zugehörigen Entity
     */
    @Override
    public String getEntityTypeName() {
        return "ChatNachrichtReferenz";
    }

    /**
     * Gibt den Anzeigenamen des zugehörigen Entity zurück.
     * 
     * @return Anzeigename der Chatreferenz
     */
    @Override
    public String getEntityDisplayName() {
        String displayName = kursMaterialName != null ? kursMaterialName : "Quelle";
        if (seitennummer != null) {
            displayName += " (S. " + seitennummer + ")";
        }
        return displayName;
    }
}