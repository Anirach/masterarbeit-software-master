package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Die Chat-Entität repräsentiert einen Chat zwischen einem Studenten und dem System.
 * Ein Chat ist immer einer Teilaufgabe zugeordnet und enthält Fragen, Erläuterungen 
 * und Nachfragen des Studenten zur Teilaufgabe.
 */
@Entity
@Getter
@Setter
public class Chat extends BaseEntity {
    
    /**
     * Der Zeitpunkt des Chat-Beginns.
     */
    @Column(nullable = false)
    private LocalDateTime zeitpunkt;
    
    /**
     * Der zugehörige Student.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    /**
     * Die zugehörige Teilaufgabe.
     * Diese Referenz darf nicht null sein, da jeder Chat genau einer Teilaufgabe zugeordnet ist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teilaufgabe_id", nullable = false)
    private Teilaufgabe teilaufgabe;
    
    /**
     * Die Nachrichten des Chats.
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("zeitpunkt ASC")
    private List<ChatNachricht> nachrichten = new ArrayList<>();
}
