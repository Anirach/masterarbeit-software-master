package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Die ChatNachrichtReferenz-Entität repräsentiert eine Referenz auf ein Kursmaterial
 * mit Seitenangabe, die einer ChatNachricht zugeordnet ist.
 */
@Entity
@Getter
@Setter
public class ChatNachrichtReferenz extends BaseEntity {
    
    /**
     * Die ChatNachricht, zu der diese Referenz gehört.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_nachricht_id", nullable = false)
    private ChatNachricht chatNachricht;
    
    /**
     * Das referenzierte Kursmaterial.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kursmaterial_id", nullable = false)
    private KursMaterial kursMaterial;
    
    /**
     * Die Seitennummer im Dokument (optional).
     */
    @Column
    private Integer seitennummer;
}