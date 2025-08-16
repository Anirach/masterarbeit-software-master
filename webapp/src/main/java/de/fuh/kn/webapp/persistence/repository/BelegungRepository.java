package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Belegung;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf Belegungs-Entitäten in der Datenbank.
 */
@Repository
public interface BelegungRepository extends JpaRepository<Belegung, Long>, JpaSpecificationExecutor<Belegung> {
    
    /**
     * Findet alle Belegungen eines bestimmten Studenten.
     *
     * @param student Der Student, dessen Belegungen gesucht werden.
     * @return Eine Liste aller Belegungen des angegebenen Studenten.
     */
    List<Belegung> findByStudent(Student student);
    
    /**
     * Findet alle Belegungen eines bestimmten Kurses.
     *
     * @param kurs Der Kurs, dessen Belegungen gesucht werden.
     * @return Eine Liste aller Belegungen des angegebenen Kurses.
     */
    List<Belegung> findByKurs(Kurs kurs);
    
    /**
     * Findet die Belegung eines bestimmten Studenten für einen bestimmten Kurs.
     *
     * @param student Der Student.
     * @param kurs Der Kurs.
     * @return Ein Optional mit der gefundenen Belegung oder ein leeres Optional, wenn keine Belegung existiert.
     */
    Optional<Belegung> findByStudentAndKurs(Student student, Kurs kurs);
    
    /**
     * Findet alle aktiven Belegungen eines Studenten (Belegungen, deren Enddatum noch nicht erreicht ist).
     *
     * @param student Der Student.
     * @param heute Das aktuelle Datum für den Vergleich.
     * @return Eine Liste aller aktiven Belegungen des Studenten.
     */
    @Query("SELECT b FROM Belegung b WHERE b.student = ?1 AND (b.endDatum IS NULL OR b.endDatum >= ?2)")
    List<Belegung> findActiveByStudent(Student student, LocalDate heute);

    /**
     * Findet alle Belegungen eines Kurses mit Paginierung.
     *
     * @param kurs Der Kurs
     * @param pageable Paginierungsinformationen
     * @return Eine Page mit Belegungen
     */
    Page<Belegung> findByKurs(Kurs kurs, Pageable pageable);
    
    /**
     * Findet Belegungen eines Kurses nach Studentennamen (Suche).
     *
     * @param kurs Der Kurs
     * @param searchTerm Suchbegriff für Studentennamen
     * @param pageable Paginierungsinformationen
     * @return Eine Page mit Belegungen
     */
    @Query("SELECT b FROM Belegung b JOIN b.student s WHERE b.kurs = ?1 AND " +
           "(LOWER(CONCAT(s.vorname, ' ', s.nachname)) LIKE LOWER(CONCAT('%', ?2, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', ?2, '%')))")
    Page<Belegung> findByKursAndStudentNameContaining(Kurs kurs, String searchTerm, Pageable pageable);
}
