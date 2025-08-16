package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Chat;
import de.fuh.kn.webapp.persistence.entity.ChatNachricht;
import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository für den Zugriff auf ChatNachricht-Entitäten in der Datenbank.
 */
@Repository
public interface ChatNachrichtRepository extends JpaRepository<ChatNachricht, Long> {
    
    /**
     * Findet alle Nachrichten eines bestimmten Chats, sortiert nach Zeitpunkt (älteste zuerst).
     *
     * @param chat Der Chat, dessen Nachrichten gesucht werden.
     * @return Eine Liste aller Nachrichten des angegebenen Chats, sortiert nach Zeitpunkt.
     */
    List<ChatNachricht> findByChatOrderByZeitpunktAsc(Chat chat);

    /**
     * Zählt die Anzahl der Nachrichten eines Studenten in einem bestimmten Kurs.
     *
     * @param student Der Student
     * @param kurs Der Kurs
     * @return Die Anzahl der Nachrichten
     */
    @Query("SELECT COUNT(cn) FROM ChatNachricht cn " +
           "JOIN cn.chat c " +
           "JOIN c.teilaufgabe ta " +
           "JOIN ta.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE c.student = ?1 AND ke.kurs = ?2 AND cn.istSystemNachricht = false")
    long countByStudentAndKurs(Student student, Kurs kurs);
    
    /**
     * Summiert die Kosten aller Chat-Nachrichten eines Studenten in einem bestimmten Kurs.
     *
     * @param student Der Student
     * @param kurs Der Kurs
     * @return Die Gesamtkosten der Chat-Nachrichten (null wenn keine Kosten vorhanden)
     */
    @Query("SELECT SUM(cn.kosten) FROM ChatNachricht cn " +
           "JOIN cn.chat c " +
           "JOIN c.teilaufgabe ta " +
           "JOIN ta.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE c.student = ?1 AND ke.kurs = ?2 AND cn.kosten IS NOT NULL")
    BigDecimal sumKostenByStudentAndKurs(Student student, Kurs kurs);
}
