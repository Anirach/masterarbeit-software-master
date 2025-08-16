package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Die Kurseinheit-Entität repräsentiert eine Lerneinheit innerhalb eines Kurses.
 * Eine Kurseinheit enthält Aufgaben und kann eigenes Kursmaterial haben.
 */
@Entity
@Getter
@Setter
public class Kurseinheit extends BaseEntity {
    
    /**
     * Der Name der Kurseinheit.
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Die Reihenfolge der Kurseinheit innerhalb des Kurses.
     */
    @Column(nullable = false)
    private Integer reihenfolge;
    
    /**
     * Der zugehörige Kurs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kurs_id", nullable = false)
    private Kurs kurs;
    
    /**
     * Das Kursmaterial der Kurseinheit.
     */
    @OneToMany(mappedBy = "kurseinheit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<KursMaterial> kursMaterialien = new ArrayList<>();
    
    /**
     * Die Aufgaben der Kurseinheit.
     */
    @OneToMany(mappedBy = "kurseinheit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("reihenfolge ASC")
    private List<Aufgabe> aufgaben = new ArrayList<>();
}
