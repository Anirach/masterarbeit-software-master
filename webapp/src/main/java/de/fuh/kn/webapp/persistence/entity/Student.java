package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Spezialisierung der Nutzer-Klasse für Studierende.
 * Studierende können Kurse belegen, Aufgaben lösen und Chats führen.
 */
@Entity
@DiscriminatorValue("STUDENT")
@Getter
@Setter
public class Student extends Nutzer {

    /**
     * Die Matrikelnummer des Studenten.
     * Wird für die Zuordnung zu Kursen verwendet.
     */
    @Column(unique = true)
    private String matrikelnummer;
    
    /**
     * Die Belegungen des Studenten.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Belegung> belegungen = new ArrayList<>();
    
    /**
     * Die Lösungsversuche des Studenten.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoesungsVersuch> loesungsVersuche = new ArrayList<>();
    
    /**
     * Die Chats des Studenten.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chat> chats = new ArrayList<>();
}
