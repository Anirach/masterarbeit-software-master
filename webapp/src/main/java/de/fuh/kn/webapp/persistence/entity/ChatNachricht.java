package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Die ChatNachricht-Entität repräsentiert eine einzelne Nachricht in einem Chat.
 * Eine Nachricht kann vom Studenten oder vom System stammen und
 * kann Bezüge zu mehreren Kursmaterialien haben.
 */
@Entity
@Getter
@Setter
public class ChatNachricht extends BaseEntity {
    
    /**
     * Der Zeitpunkt der Nachricht.
     */
    @Column(nullable = false)
    private LocalDateTime zeitpunkt;
    
    /**
     * Der Inhalt der Nachricht.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String inhalt;
    
    /**
     * Gibt an, ob die Nachricht vom System oder vom Studenten stammt.
     */
    @Column(nullable = false)
    private Boolean istSystemNachricht;
    
    /**
     * Der zugehörige Chat.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * Die referenzierten Kursmaterialien mit Seitenangaben.
     */
    @OneToMany(mappedBy = "chatNachricht", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatNachrichtReferenz> referenzen = new ArrayList<>();
    
    /**
     * Die Anzahl der Input-Token, die für diese Nachricht verwendet wurden (nur für System-Nachrichten).
     */
    @Column
    private Integer inputToken;
    
    /**
     * Die Anzahl der Output-Token, die für diese Nachricht generiert wurden (nur für System-Nachrichten).
     */
    @Column
    private Integer outputToken;
    
    /**
     * Das für die Generierung verwendete Modell (nur für System-Nachrichten).
     */
    @Column
    private String modell;
    
    /**
     * Die berechneten Kosten für die Nachricht in USD (nur für System-Nachrichten).
     */
    @Column(precision = 10, scale = 6)
    private BigDecimal kosten;
}