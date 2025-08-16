package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Die Aufgabe-Entität repräsentiert eine Übungsaufgabe innerhalb einer Kurseinheit.
 * Eine Aufgabe kann aus einer einzelnen Teilaufgabe oder mehreren Teilaufgaben bestehen.
 */
@Entity
@Getter
@Setter
public class Aufgabe extends BaseEntity {
    
    /**
     * Der Titel der Aufgabe.
     */
    @Column(nullable = false)
    private String titel;
    
    /**
     * Die Reihenfolge der Aufgabe innerhalb der Kurseinheit.
     */
    @Column(nullable = false)
    private Integer reihenfolge;
    
    /**
     * Der allgemeine Aufgabentext für Aufgaben mit mehreren Teilaufgaben.
     * Bei einfachen Aufgaben ist dieser null.
     */
    @Column(columnDefinition = "TEXT")
    private String aufgabenText;
    
    /**
     * Die zugehörige Kurseinheit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kurseinheit_id", nullable = false)
    private Kurseinheit kurseinheit;
    
    /**
     * Die Teilaufgaben der Aufgabe.
     */
    @OneToMany(mappedBy = "aufgabe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("reihenfolge ASC")
    private List<Teilaufgabe> teilaufgaben = new ArrayList<>();

    /**
     * Prüft, ob es sich um eine einfache Aufgabe handelt (mit nur einer Teilaufgabe).
     * 
     * @return true, wenn die Aufgabe genau eine Teilaufgabe hat, sonst false
     */
    @Transient
    public boolean isEinfach() {
        return teilaufgaben.size() <= 1;
    }
}
