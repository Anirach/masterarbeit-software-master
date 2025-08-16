package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity-Klasse zur Protokollierung von Benutzeraktivitäten im System.
 * Speichert Informationen über durchgeführte Aktionen wie Login, Aufgabenbearbeitung, Kursverwaltung etc.
 */
@Entity
@Getter
@Setter
public class Aktivitaet extends BaseEntity {

    /**
     * Der Zeitpunkt, zu dem die Aktivität stattgefunden hat.
     */
    @Column(nullable = false)
    private LocalDateTime zeitpunkt;

    /**
     * Der Nutzer, der die Aktivität ausgeführt hat.
     */
    @ManyToOne
    @JoinColumn(name = "nutzer_id")
    private Nutzer nutzer;

    /**
     * Der Typ der Aktivität, kategorisiert in verschiedene Aktivitätsbereiche.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AktivitaetsTyp aktivitaetsTyp;

    /**
     * Eine textuelle Beschreibung der durchgeführten Aktivität.
     */
    @Column(length = 1000)
    private String beschreibung;

    /**
     * Zusätzliche strukturierte Informationen zur Aktivität im JSON-Format.
     * Kann für erweiterte Informationen wie betroffene Objekte, Parameter etc. verwendet werden.
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Flag, das angibt, ob die Aktion erfolgreich durchgeführt wurde.
     */
    @Column(nullable = false)
    private Boolean erfolg;
    
    /**
     * Der Klassenname des referenzierten Objekts (z.B. "Kurseinheit", "LoesungsVersuch").
     * Teil der polymorphen Beziehung zu anderen Entitäten.
     */
    @Column(name = "referenz_typ")
    private String referenzTyp;
    
    /**
     * Die ID des referenzierten Objekts.
     * Teil der polymorphen Beziehung zu anderen Entitäten.
     */
    @Column(name = "referenz_id")
    private Long referenzId;
}
