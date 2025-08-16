package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Chat;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository für den Zugriff auf Chat-Entitäten in der Datenbank.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    
    /**
     * Findet alle Chats eines bestimmten Studenten.
     *
     * @param student Der Student, dessen Chats gesucht werden.
     * @return Eine Liste aller Chats des angegebenen Studenten.
     */
    List<Chat> findByStudent(Student student);

    /**
     * Findet alle Chats eines bestimmten Studenten zu einer bestimmten Teilaufgabe.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return Eine Liste aller Chats des angegebenen Studenten zur angegebenen Teilaufgabe.
     */
    List<Chat> findByStudentAndTeilaufgabe(Student student, Teilaufgabe teilaufgabe);
    
    /**
     * Findet alle Chats eines bestimmten Studenten, sortiert nach dem Zeitpunkt (neueste zuerst).
     *
     * @param student Der Student.
     * @return Eine Liste aller Chats des angegebenen Studenten, sortiert nach Zeitpunkt.
     */
    List<Chat> findByStudentOrderByZeitpunktDesc(Student student);
    
    /**
     * Findet alle Chats eines Studenten in einem bestimmten Kurs.
     *
     * @param student Der Student.
     * @param kurs Der Kurs.
     * @return Eine Liste aller Chats des Studenten im angegebenen Kurs.
     */
    @Query("SELECT c FROM Chat c " +
           "JOIN c.teilaufgabe t " +
           "JOIN t.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE c.student = :student AND ke.kurs = :kurs " +
           "ORDER BY ke.reihenfolge ASC, a.reihenfolge ASC, t.reihenfolge ASC")
    List<Chat> findByStudentAndKurs(@Param("student") Student student, @Param("kurs") Kurs kurs);
}
