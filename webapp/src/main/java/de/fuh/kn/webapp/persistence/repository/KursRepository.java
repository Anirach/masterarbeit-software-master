package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Kurs-Entitäten in der Datenbank.
 */
@Repository
public interface KursRepository extends JpaRepository<Kurs, Long> {
    
    /**
     * Findet einen Kurs anhand seines Namens.
     *
     * @param name Der Name des Kurses.
     * @return Ein Optional mit dem gefundenen Kurs oder ein leeres Optional, wenn kein Kurs mit dem Namen existiert.
     */
    Optional<Kurs> findByName(String name);
    
    /**
     * Findet alle Kurse, sortiert nach ihrem Namen.
     *
     * @return Eine Liste aller Kurse, sortiert nach Namen.
     */
    List<Kurs> findAllByOrderByNameAsc();
    
    /**
     * Findet alle Kurse, die von einem bestimmten Studenten belegt werden.
     *
     * @param studentId Die ID des Studenten.
     * @return Eine Liste aller Kurse, die der Student belegt.
     */
    @Query("SELECT k FROM Kurs k JOIN Belegung b ON b.kurs = k WHERE b.student.id = ?1")
    List<Kurs> findAllByStudentId(Long studentId);
    
    /**
     * Überprüft, ob ein Kurs mit dem angegebenen Namen existiert.
     *
     * @param name Der Name des Kurses.
     * @return True, wenn ein Kurs mit dem Namen existiert, sonst False.
     */
    boolean existsByName(String name);
}
