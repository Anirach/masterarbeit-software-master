package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Die Kurs-Entität repräsentiert einen Kurs im System.
 * Ein Kurs besteht aus mehreren Kurseinheiten und kann Kursmaterial enthalten.
 */
@Entity
@Getter
@Setter
public class Kurs extends BaseEntity {
    
    /**
     * Der Name des Kurses.
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Die Kurseinheiten des Kurses.
     */
    @OneToMany(mappedBy = "kurs", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("reihenfolge ASC")
    private List<Kurseinheit> kurseinheiten = new ArrayList<>();
    
    /**
     * Das Kursmaterial des Kurses.
     */
    @OneToMany(mappedBy = "kurs", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<KursMaterial> kursMaterialien = new ArrayList<>();
    
    /**
     * Die Belegungen des Kurses.
     */
    @OneToMany(mappedBy = "kurs", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Belegung> belegungen = new ArrayList<>();
}
