package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Student-Entitäten in der Datenbank.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    /**
     * Findet einen Studenten anhand seiner Matrikelnummer.
     *
     * @param matrikelnummer Die Matrikelnummer des Studenten.
     * @return Ein Optional mit dem gefundenen Studenten oder ein leeres Optional, wenn kein Student mit der Matrikelnummer existiert.
     */
    Optional<Student> findByMatrikelnummer(String matrikelnummer);
    
    /**
     * Überprüft, ob ein Student mit der angegebenen Matrikelnummer existiert.
     *
     * @param matrikelnummer Die Matrikelnummer des Studenten.
     * @return True, wenn ein Student mit der Matrikelnummer existiert, sonst False.
     */
    boolean existsByMatrikelnummer(String matrikelnummer);
    
    /**
     * Findet einen Studenten anhand seiner E-Mail-Adresse.
     *
     * @param email Die E-Mail-Adresse des Studenten.
     * @return Ein Optional mit dem gefundenen Studenten oder ein leeres Optional, wenn kein Student mit der E-Mail-Adresse existiert.
     */
    Optional<Student> findByEmail(String email);
    
    /**
     * Findet alle Studenten, die einen bestimmten Kurs belegen.
     *
     * @param kurs Der Kurs, dessen Studenten gesucht werden.
     * @return Eine Liste aller Studenten, die den angegebenen Kurs belegen.
     */
    @Query("SELECT s FROM Student s JOIN Belegung b ON b.student = s WHERE b.kurs = ?1")
    List<Student> findAllByKurs(Kurs kurs);
    
    /**
     * Findet alle Studenten, sortiert nach Nachname und Vorname.
     *
     * @return Eine Liste aller Studenten, sortiert nach Namen.
     */
    List<Student> findAllByOrderByNachnameAscVornameAsc();
}
