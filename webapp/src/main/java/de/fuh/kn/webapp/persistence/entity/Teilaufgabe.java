package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Teilaufgabe-Entität repräsentiert eine Teilaufgabe innerhalb einer Aufgabe.
 * Enthält die Aufgabenstellung im Markdown-Format und die Musterlösung.
 */
@Entity
@Getter
@Setter
public class Teilaufgabe extends BaseEntity {
    
    /**
     * Die Reihenfolge der Teilaufgabe innerhalb der Aufgabe.
     */
    @Column(nullable = false)
    private Integer reihenfolge;
    
    /**
     * Die Aufgabenstellung im Markdown-Format.
     * Enthält auch die Platzhalter für die Eingabefelder.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String aufgabenstellungMarkdown;
    
    /**
     * Die Musterlösung für die Felder der Aufgabe.
     * Schlüssel ist der Name des Feldes, Wert ist die erwartete Lösung.
     */
    @ElementCollection
    @CollectionTable(name = "teilaufgabe_musterloesungen", 
        joinColumns = @JoinColumn(name = "teilaufgabe_id"))
    @MapKeyColumn(name = "feld_name")
    @Column(name = "loesung", columnDefinition = "TEXT")
    private Map<String, String> musterloesungFelder = new HashMap<>();
    
    /**
     * Die Bewertungshinweise für die Aufgabe.
     */
    @Column(columnDefinition = "TEXT")
    private String musterloesungBewertungshinweise;
    
    /**
     * Die zugehörige Aufgabe.
     * Diese Referenz darf nicht null sein und muss vor dem Speichern gesetzt werden.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aufgabe_id", nullable = false)
    private Aufgabe aufgabe;
    
    /**
     * Die Lösungsversuche zu dieser Teilaufgabe.
     */
    @OneToMany(mappedBy = "teilaufgabe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoesungsVersuch> loesungsVersuche = new ArrayList<>();
    
    /**
     * Die Chats zu dieser Teilaufgabe.
     */
    @OneToMany(mappedBy = "teilaufgabe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chat> chats = new ArrayList<>();
}
