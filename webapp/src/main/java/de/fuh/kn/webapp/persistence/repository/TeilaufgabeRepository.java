package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Teilaufgabe-Entitäten in der Datenbank.
 */
@Repository
public interface TeilaufgabeRepository extends JpaRepository<Teilaufgabe, Long> {
    
    /**
     * Findet alle Teilaufgaben einer bestimmten Aufgabe.
     *
     * @param aufgabe Die Aufgabe, deren Teilaufgaben gesucht werden.
     * @return Eine Liste aller Teilaufgaben der angegebenen Aufgabe.
     */
    List<Teilaufgabe> findByAufgabe(Aufgabe aufgabe);
    
    /**
     * Findet alle Teilaufgaben einer bestimmten Aufgabe, sortiert nach Reihenfolge.
     *
     * @param aufgabe Die Aufgabe, deren Teilaufgaben gesucht werden.
     * @return Eine Liste aller Teilaufgaben der angegebenen Aufgabe, sortiert nach Reihenfolge.
     */
    List<Teilaufgabe> findByAufgabeOrderByReihenfolgeAsc(Aufgabe aufgabe);
    
    /**
     * Findet die nächste Teilaufgabe nach einer gegebenen Reihenfolgenummer innerhalb einer Aufgabe.
     *
     * @param aufgabe Die Aufgabe, in der gesucht wird.
     * @param reihenfolge Die Reihenfolgenummer, nach der die nächste Teilaufgabe gesucht wird.
     * @return Ein Optional mit der nächsten Teilaufgabe oder ein leeres Optional, wenn keine folgende Teilaufgabe existiert.
     */
    Optional<Teilaufgabe> findFirstByAufgabeAndReihenfolgeGreaterThanOrderByReihenfolgeAsc(Aufgabe aufgabe, int reihenfolge);
    
    /**
     * Zählt die Anzahl der Teilaufgaben einer bestimmten Aufgabe.
     *
     * @param aufgabe Die Aufgabe, deren Teilaufgaben gezählt werden sollen.
     * @return Die Anzahl der Teilaufgaben der angegebenen Aufgabe.
     */
    long countByAufgabe(Aufgabe aufgabe);

    /**
     * Zählt die Anzahl der Teilaufgaben für einen bestimmten Kurs.
     *
     * @param kursId Die ID des Kurses, für den die Teilaufgaben gezählt werden sollen.
     * @return Die Anzahl der Teilaufgaben für den angegebenen Kurs.
     */
    @Query("SELECT COUNT(t) FROM Teilaufgabe t JOIN t.aufgabe a JOIN a.kurseinheit ke WHERE ke.kurs.id = :kursId")
    long countTeilaufgabenByKursId(@Param("kursId") Long kursId);
    
    /**
     * Findet alle Teilaufgaben eines Kurses, sortiert nach Kurseinheit, Aufgabe und Teilaufgabe.
     *
     * @param kurs Der Kurs, dessen Teilaufgaben gesucht werden.
     * @return Eine Liste aller Teilaufgaben des angegebenen Kurses, sortiert nach Hierarchie.
     */
    @Query("SELECT t FROM Teilaufgabe t " +
           "JOIN t.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE ke.kurs = :kurs " +
           "ORDER BY ke.reihenfolge ASC, a.reihenfolge ASC, t.reihenfolge ASC")
    List<Teilaufgabe> findByKursOrderByHierarchy(@Param("kurs") Kurs kurs);
}
