package de.fuh.kn.webapp.persistence.entity;

import de.fuh.kn.webapp.persistence.listener.KursMaterialEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Die KursMaterial-Entität repräsentiert ein Dokument oder Bild,
 * das einem Kurs oder einer Kurseinheit zugeordnet ist.
 */
@Entity
@Getter
@Setter
@EntityListeners(KursMaterialEntityListener.class)
public class KursMaterial extends BaseEntity {
    
    /**
     * Der Name des Kursmaterials.
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Der Typ des Kursmaterials (z.B. "DOKUMENT" oder "BILD").
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private KursMaterialTyp typ;
    
    /**
     * Der binäre Inhalt des Dokuments oder Bildes.
     * Wird für Downloads und die Anzeige verwendet.
     */
    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] inhalt;
    
    /**
     * Der MIME-Typ des Inhalts (z.B. "application/pdf", "image/png").
     * Wird für korrekte Downloads und Anzeige benötigt.
     */
    @Column(nullable = false)
    private String mimeType;

    /**
     * Gibt an, ob das Material bereits im Vektorspeicher indexiert wurde.
     * Nur relevant für Dokumente (Typ = DOKUMENT).
     */
    @Column(nullable = false)
    private Boolean indexiert = false;

    /**
     * Die zugehörige Kurseinheit (kann null sein, wenn das Material zum Kurs gehört).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kurseinheit_id")
    private Kurseinheit kurseinheit;
    
    /**
     * Der zugehörige Kurs (kann null sein, wenn das Material zur Kurseinheit gehört).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kurs_id")
    private Kurs kurs;

    /**
     * Die ChatNachrichtReferenzen, die auf dieses Kursmaterial verweisen.
     */
    @OneToMany(mappedBy = "kursMaterial", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<ChatNachrichtReferenz> chatNachrichtReferenzen = new ArrayList<>();
    
    /**
     * Der Enum für den Typ des Kursmaterials.
     */
    public enum KursMaterialTyp {
        DOKUMENT,
        BILD
    }
}
