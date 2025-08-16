package de.fuh.kn.webapp.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Die Belegung-Entität repräsentiert die Zuordnung eines Studenten zu einem Kurs.
 * Die Belegung ist zeitlich begrenzt und kann von der Kursbetreuung verwaltet werden.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "kurs_id"}))
@Getter
@Setter
public class Belegung extends BaseEntity {
    
    /**
     * Der Student, der den Kurs belegt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    /**
     * Der belegte Kurs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kurs_id", nullable = false)
    private Kurs kurs;
    
    /**
     * Das Startdatum der Belegung.
     */
    @Column(nullable = false)
    private LocalDate startDatum;
    
    /**
     * Das Enddatum der Belegung.
     * Wenn null, ist die Belegung zeitlich unbegrenzt.
     */
    @Column
    private LocalDate endDatum;
}
