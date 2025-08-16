package de.fuh.kn.webapp.persistence.repository;

import de.fuh.kn.webapp.persistence.entity.Kurs;
import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.entity.Student;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository für den Zugriff auf LoesungsVersuch-Entitäten in der Datenbank.
 */
@Repository
public interface LoesungsVersuchRepository extends JpaRepository<LoesungsVersuch, Long> {

    /**
     * Findet alle Lösungsversuche eines bestimmten Studenten.
     *
     * @param student Der Student, dessen Lösungsversuche gesucht werden.
     * @return Eine Liste aller Lösungsversuche des angegebenen Studenten.
     */
    List<LoesungsVersuch> findByStudent(Student student);

    /**
     * Zählt die Anzahl der korrekten Lösungsversuche eines Studenten.
     *
     * @param studentId Die ID des Studenten.
     * @param korrekt Flag, ob die Lösung korrekt sein soll.
     * @return Die Anzahl der korrekten Lösungsversuche des Studenten.
     */
    @Query("SELECT COUNT(DISTINCT lv.teilaufgabe) FROM LoesungsVersuch lv WHERE lv.student.id = ?1 AND lv.istAbgeschlossen = ?2 AND lv.istZurueckGesetzt = false")
    Long countByStudentIdAndKorrekt(Long studentId, boolean korrekt);

    /**
     * Findet alle Lösungsversuche zu einer bestimmten Teilaufgabe.
     *
     * @param teilaufgabe Die Teilaufgabe, zu der die Lösungsversuche gesucht werden.
     * @return Eine Liste aller Lösungsversuche zur angegebenen Teilaufgabe.
     */
    List<LoesungsVersuch> findByTeilaufgabe(Teilaufgabe teilaufgabe);

    /**
     * Findet alle Lösungsversuche eines bestimmten Studenten zu einer bestimmten Teilaufgabe.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return Eine Liste aller Lösungsversuche des angegebenen Studenten zur angegebenen Teilaufgabe.
     */
    List<LoesungsVersuch> findByStudentAndTeilaufgabe(Student student, Teilaufgabe teilaufgabe);

    /**
     * Findet alle Lösungsversuche eines bestimmten Studenten zu einer bestimmten Teilaufgabe,
     * sortiert nach dem Zeitpunkt absteigend (neueste zuerst).
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return Eine sortierte Liste aller Lösungsversuche des angegebenen Studenten zur angegebenen Teilaufgabe.
     */
    List<LoesungsVersuch> findByStudentAndTeilaufgabeOrderByZeitpunktDesc(Student student, Teilaufgabe teilaufgabe);

    /**
     * Findet den letzten Lösungsversuch eines Studenten für eine Teilaufgabe, der nicht zurückgesetzt wurde.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return Ein Optional mit dem neuesten nicht zurückgesetzten Lösungsversuch oder ein leeres Optional, wenn kein solcher Versuch existiert.
     */
    @Query("SELECT lv FROM LoesungsVersuch lv WHERE lv.student = ?1 AND lv.teilaufgabe = ?2 AND lv.istZurueckGesetzt = false ORDER BY lv.zeitpunkt DESC")
    Optional<LoesungsVersuch> findLatestValidByStudentAndTeilaufgabe(Student student, Teilaufgabe teilaufgabe);

    /**
     * Findet den letzten Lösungsversuch eines Studenten für eine Teilaufgabe, unabhängig vom Zurücksetzungsstatus.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return Ein Optional mit dem neuesten Lösungsversuch oder ein leeres Optional, wenn kein Versuch existiert.
     */
    Optional<LoesungsVersuch> findFirstByStudentAndTeilaufgabeOrderByZeitpunktDesc(Student student, Teilaufgabe teilaufgabe);

    /**
     * Überprüft, ob ein Student eine Teilaufgabe erfolgreich abgeschlossen hat.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return True, wenn der Student die Teilaufgabe erfolgreich abgeschlossen hat, sonst False.
     */
    boolean existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(Student student, Teilaufgabe teilaufgabe);

    /**
     * Überprüft, ob ein Student eine Teilaufgabe erfolgreich abgeschlossen hat (ID-basiert).
     *
     * @param studentId Die ID des Studenten.
     * @param teilaufgabeId Die ID der Teilaufgabe.
     * @return True, wenn der Student die Teilaufgabe erfolgreich abgeschlossen hat, sonst False.
     */
    boolean existsByStudentIdAndTeilaufgabeIdAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(Long studentId, Long teilaufgabeId);

    /**
     * Überprüft, ob ein Student eine Teilaufgabe übersprungen hat.
     *
     * @param student Der Student.
     * @param teilaufgabe Die Teilaufgabe.
     * @return True, wenn der Student die Teilaufgabe übersprungen hat, sonst False.
     */
    boolean existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(Student student, Teilaufgabe teilaufgabe);

    /**
     * Überprüft, ob ein Student eine Teilaufgabe übersprungen hat (ID-basiert).
     *
     * @param studentId Die ID des Studenten.
     * @param teilaufgabeId Die ID der Teilaufgabe.
     * @return True, wenn der Student die Teilaufgabe übersprungen hat, sonst False.
     */
    boolean existsByStudentIdAndTeilaufgabeIdAndIstUebersprungenTrue(Long studentId, Long teilaufgabeId);

    /**
     * Findet alle Lösungsversuche eines Studenten, die übersprungen aber nicht abgeschlossen sind.
     * Diese Methode wird genutzt, um beim Login diese Versuche zurückzusetzen.
     *
     * @param student Der Student.
     * @return Liste aller übersprungenen und nicht abgeschlossenen Lösungsversuche.
     */
    List<LoesungsVersuch> findByStudentAndIstUebersprungenTrueAndIstAbgeschlossenFalse(Student student);

    /**
     * Berechnet die durchschnittliche Punktzahl für eine Aufgabe.
     *
     * @param teilaufgabe Die Teilaufgabe.
     * @return Die durchschnittliche Punktzahl für die angegebene Aufgabe.
     */
    @Query("SELECT AVG(lv.bewertungPunkte) FROM LoesungsVersuch lv WHERE lv.teilaufgabe = ?1")
    Double calculateAverageScoreForAufgabe(Teilaufgabe teilaufgabe);

    /**
     * Berechnet die durchschnittliche Punktzahl für eine Teilaufgabe.
     *
     * @param teilaufgabe Die Teilaufgabe.
     * @return Die durchschnittliche Punktzahl für die angegebene Teilaufgabe.
     */
    @Query("SELECT AVG(lv.bewertungPunkte) FROM LoesungsVersuch lv WHERE lv.teilaufgabe = ?1")
    Double calculateAverageScoreForTeilaufgabe(Teilaufgabe teilaufgabe);

    /**
     * Findet die Bewertungspunkte des letzten Lösungsversuchs eines Studenten für eine Teilaufgabe.
     *
     * @param studentId Die ID des Studenten.
     * @param teilaufgabeId Die ID der Teilaufgabe.
     * @return Eine Liste mit dem neuesten Bewertungspunkten oder eine leere Liste, wenn keine vorhanden sind.
     */
    @Query("SELECT lv.bewertungPunkte FROM LoesungsVersuch lv " +
           "WHERE lv.student.id = :studentId AND lv.teilaufgabe.id = :teilaufgabeId " +
           "AND lv.bewertungPunkte IS NOT NULL " +
           "AND lv.istZurueckGesetzt = false " +
           "ORDER BY lv.zeitpunkt DESC")
    List<Integer> findBewertungspunkteByStudentIdAndTeilaufgabeId(Long studentId, Long teilaufgabeId);

    /**
     * Findet alle IDs von Teilaufgaben, die ein Student abgeschlossen hat und die in der angegebenen Liste enthalten sind.
     * Diese Methode ist für eine Batch-Verarbeitung optimiert, um mehrere Teilaufgaben auf einmal zu überprüfen.
     *
     * @param studentId Die ID des Studenten
     * @param teilaufgabenIds Liste der zu überprüfenden Teilaufgaben-IDs
     * @return Liste der IDs von Teilaufgaben, die der Student abgeschlossen hat
     */
    @Query("SELECT DISTINCT lv.teilaufgabe.id FROM LoesungsVersuch lv " +
           "WHERE lv.student.id = :studentId AND lv.teilaufgabe.id IN :teilaufgabenIds " +
           "AND lv.istAbgeschlossen = true " +
           "AND lv.istZurueckGesetzt = false")
    List<Long> findAbgeschlosseneTeilaufgabenIdsByStudentId(Long studentId, List<Long> teilaufgabenIds);
    
    /**
     * Findet die letzten Lösungsversuche eines Studenten in einem Kurs, sortiert nach Zeitpunkt.
     *
     * @param student Der Student
     * @param kurs Der Kurs
     * @return Liste mit Lösungsversuchen, neueste zuerst
     */
    @Query("SELECT lv FROM LoesungsVersuch lv " +
           "JOIN lv.teilaufgabe ta " +
           "JOIN ta.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE lv.student = ?1 AND ke.kurs = ?2 " +
           "ORDER BY lv.zeitpunkt DESC")
    List<LoesungsVersuch> findByStudentAndKursOrderByZeitpunktDesc(Student student, Kurs kurs);
    
    /**
     * Summiert die Kosten aller Lösungsversuche eines Studenten in einem bestimmten Kurs.
     *
     * @param student Der Student
     * @param kurs Der Kurs
     * @return Die Gesamtkosten der Lösungsversuche (null wenn keine Kosten vorhanden)
     */
    @Query("SELECT SUM(lv.kosten) FROM LoesungsVersuch lv " +
           "JOIN lv.teilaufgabe ta " +
           "JOIN ta.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE lv.student = ?1 AND ke.kurs = ?2 AND lv.kosten IS NOT NULL")
    BigDecimal sumKostenByStudentAndKurs(Student student, Kurs kurs);

    /**
     * Zählt die Anzahl der korrekten Lösungsversuche eines Studenten in einem bestimmten Kurs.
     *
     * @param studentId Die ID des Studenten.
     * @param kursId Die ID des Kurses.
     * @param korrekt Flag, ob die Lösung korrekt sein soll.
     * @return Die Anzahl der korrekten Lösungsversuche des Studenten im angegebenen Kurs.
     */
    @Query("SELECT COUNT(DISTINCT lv.teilaufgabe) FROM LoesungsVersuch lv " +
           "JOIN lv.teilaufgabe ta " +
           "JOIN ta.aufgabe a " +
           "JOIN a.kurseinheit ke " +
           "WHERE lv.student.id = ?1 AND ke.kurs.id = ?2 AND lv.istAbgeschlossen = ?3 AND lv.istZurueckGesetzt = false")
    Long countByStudentIdAndKursIdAndKorrekt(Long studentId, Long kursId, boolean korrekt);
}
