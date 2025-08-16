package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Aufgabe;
import de.fuh.kn.webapp.persistence.entity.Kurseinheit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Aufgaben-Entitäten in der Datenbank.
 */
@Repository
public interface AufgabeRepository extends JpaRepository<Aufgabe, Long> {

    /**
     * Findet alle Aufgaben, die einer bestimmten Kurseinheit zugeordnet sind.
     *
     * @param kurseinheit Die Kurseinheit, zu der die Aufgaben gehören.
     * @return Eine Liste aller Aufgaben der angegebenen Kurseinheit.
     */
    List<Aufgabe> findByKurseinheit(Kurseinheit kurseinheit);

    /**
     * Findet alle Aufgaben, die einer bestimmten Kurseinheit zugeordnet sind, sortiert nach Reihenfolge.
     *
     * @param kurseinheit Die Kurseinheit, zu der die Aufgaben gehören.
     * @return Eine Liste aller Aufgaben der angegebenen Kurseinheit, sortiert nach Reihenfolge.
     */
    List<Aufgabe> findByKurseinheitOrderByReihenfolgeAsc(Kurseinheit kurseinheit);

    /**
     * Findet eine Aufgabe anhand ihres Titels und der zugehörigen Kurseinheit.
     *
     * @param titel Der Titel der Aufgabe.
     * @param kurseinheit Die Kurseinheit, zu der die Aufgabe gehört.
     * @return Die gefundene Aufgabe oder null, wenn keine entsprechende Aufgabe existiert.
     */
    Aufgabe findByTitelAndKurseinheit(String titel, Kurseinheit kurseinheit);

    /**
     * Findet die Aufgabe, zu der eine bestimmte Teilaufgabe gehört.
     *
     * @param teilaufgabeId Die ID der Teilaufgabe
     * @return Optional mit der gefundenen Aufgabe oder leeres Optional, wenn keine Aufgabe gefunden wurde
     */
    @Query("SELECT a FROM Aufgabe a JOIN a.teilaufgaben t WHERE t.id = :teilaufgabeId")
    Optional<Aufgabe> findByTeilaufgabenId(@Param("teilaufgabeId") Long teilaufgabeId);

    /**
     * Findet alle Aufgaben eines Kurses, sortiert nach Kurseinheit und Reihenfolge.
     *
     * @param kursId Die ID des Kurses
     * @return Eine Liste aller Aufgaben des Kurses, sortiert nach Kurseinheit und Reihenfolge
     */
    @Query("SELECT a FROM Aufgabe a WHERE a.kurseinheit.kurs.id = :kursId ORDER BY a.kurseinheit.reihenfolge, a.reihenfolge")
    List<Aufgabe> findByKursIdOrderByKurseinheitAndReihenfolge(@Param("kursId") Long kursId);
}
